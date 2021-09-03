package com.example.kotlincamerafase

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.kotlincamerafase.camera.GraphicOverlay
import com.google.android.gms.vision.MultiProcessor
import com.example.kotlincamerafase.FaceGraphic
import com.example.kotlincamerafase.GraphicTracker
import com.example.kotlincamerafase.TrackedGraphic
import kotlin.jvm.Volatile
import androidx.annotation.RequiresApi
import android.os.Build
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face

/**
 * Factory for creating a tracker and associated graphic to be associated with a new face.  The
 * multi-processor uses this factory to create face trackers as needed -- one for each individual.
 */
internal class FaceTrackerFactory(private val mGraphicOverlay: GraphicOverlay) :
    MultiProcessor.Factory<Face> {
    override fun create(face: Face): GraphicTracker<Face?> {
        val graphic = FaceGraphic(mGraphicOverlay)
        return GraphicTracker(mGraphicOverlay, graphic)
    }
}

/**
 * Graphic instance for rendering face position, size, and ID within an associated graphic overlay
 * view.
 */
internal class FaceGraphic(overlay: GraphicOverlay?) : TrackedGraphic<Face?>(overlay) {
    private val mFacePositionPaint: Paint
    private val mIdPaint: Paint
    private val mBoxPaint: Paint

    @Volatile
    private var mFace: Face? = null

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    override fun updateItem(item: Face?) {
        mFace = item
        postInvalidate()
    }

    /**
     * Draws the face annotations for position, size, and ID on the supplied canvas.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun draw(canvas: Canvas?) {
        val face = mFace ?: return

        // Draws a circle at the position of the detected face, with the face's track id below.
        val cx = translateX(face.position.x + face.width / 2)
        val cy = translateY(face.position.y + face.height / 2)
        canvas!!.drawCircle(cx, cy, FACE_POSITION_RADIUS, mFacePositionPaint)
        canvas.drawText("id: $id", cx + ID_X_OFFSET, cy + ID_Y_OFFSET, mIdPaint)

        // Draws an oval around the face.
        val xOffset = scaleX(face.width / 2.0f)
        val yOffset = scaleY(face.height / 2.0f)
        val left = cx - xOffset
        val top = cy - yOffset
        val right = cx + xOffset
        val bottom = cy + yOffset
        canvas.drawOval(left, top, right, bottom, mBoxPaint)
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 10.0f
        private const val ID_TEXT_SIZE = 40.0f
        private const val ID_Y_OFFSET = 50.0f
        private const val ID_X_OFFSET = -50.0f
        private const val BOX_STROKE_WIDTH = 5.0f
        private val COLOR_CHOICES = intArrayOf(Color.MAGENTA, Color.RED, Color.YELLOW)
        private var mCurrentColorIndex = 0
    }

    init {
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.size
        val selectedColor = COLOR_CHOICES[mCurrentColorIndex]
        mFacePositionPaint = Paint()
        mFacePositionPaint.color = selectedColor
        mIdPaint = Paint()
        mIdPaint.color = selectedColor
        mIdPaint.textSize = ID_TEXT_SIZE
        mBoxPaint = Paint()
        mBoxPaint.color = selectedColor
        mBoxPaint.style = Paint.Style.STROKE
        mBoxPaint.strokeWidth = BOX_STROKE_WIDTH
    }
}