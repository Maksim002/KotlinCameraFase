package com.example.kotlincamerafase

import com.example.kotlincamerafase.camera.GraphicOverlay
import com.example.kotlincamerafase.TrackedGraphic
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.Tracker

/**
 * Generic tracker which is used for tracking either a face or a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
internal class GraphicTracker<T>(
    private val mOverlay: GraphicOverlay,
    private val mGraphic: TrackedGraphic<T>
) : Tracker<T>() {
    /**
     * Start tracking the detected item instance within the item overlay.
     */
    override fun onNewItem(id: Int, item: T) {
        mGraphic.id = id
    }

    /**
     * Update the position/characteristics of the item within the overlay.
     */
    override fun onUpdate(detectionResults: Detections<T>, item: T) {
        mOverlay.add(mGraphic)
        mGraphic.updateItem(item)
    }

    /**
     * Hide the graphic when the corresponding face was not detected.  This can happen for
     * intermediate frames temporarily, for example if the face was momentarily blocked from
     * view.
     */
    override fun onMissing(detectionResults: Detections<T>) {
        mOverlay.remove(mGraphic)
    }

    /**
     * Called when the item is assumed to be gone for good. Remove the graphic annotation from
     * the overlay.
     */
    override fun onDone() {
        mOverlay.remove(mGraphic)
    }
}