package com.example.kotlincamerafase.main

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.kotlincamerafase.BarcodeTrackerFactory
import com.example.kotlincamerafase.FaceTrackerFactory
import com.example.kotlincamerafase.R
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.MultiDetector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.main.*
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private var mCameraSource: CameraSource? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource()
        } else {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission")
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            ActivityCompat.requestPermissions(
                this, permissions,
                RC_HANDLE_CAMERA_PERM
            )
            return
        }
        val thisActivity: Activity = this
        val listener = View.OnClickListener {
            ActivityCompat.requestPermissions(
                thisActivity, permissions,
                RC_HANDLE_CAMERA_PERM
            )
        }
        Snackbar.make(
            faceOverlay!!, R.string.permission_camera_rationale,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.ok, listener)
            .show()
    }

    private fun createCameraSource() {
        val context = applicationContext

        val faceDetector = FaceDetector.Builder(context).build()
        val faceFactory = FaceTrackerFactory(faceOverlay)
        faceDetector.setProcessor(
            MultiProcessor.Builder(faceFactory).build()
        )

        val barcodeDetector = BarcodeDetector.Builder(context).build()
        val barcodeFactory = BarcodeTrackerFactory(faceOverlay)
        barcodeDetector.setProcessor(
            MultiProcessor.Builder(barcodeFactory).build()
        )

        val multiDetector = MultiDetector.Builder()
            .add(faceDetector)
            .add(barcodeDetector)
            .build()
        if (!multiDetector.isOperational) {
            Log.w(TAG, "Detector dependencies are not yet available.")
            val lowstorageFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, lowstorageFilter) != null
            if (hasLowStorage) {
                Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show()
                Log.w(TAG, getString(R.string.low_storage_error))
            }
        }

        mCameraSource = CameraSource.Builder(applicationContext, multiDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(1600, 1024)
            .setRequestedFps(15.0f)
            .build()
    }

    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onPause() {
        super.onPause()
        preview!!.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mCameraSource != null) {
            mCameraSource!!.release()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Got unexpected permission result: $requestCode")
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Camera permission granted - initialize the camera source")
            createCameraSource()
            return
        }
        Log.e(
            TAG, "Permission not granted: results len = " + grantResults.size +
                    " Result code = " + if (grantResults.isNotEmpty()) grantResults[0] else "(empty)"
        )

        val listener = DialogInterface.OnClickListener { dialog, id -> finish() }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Multitracker sample")
            .setMessage(R.string.no_camera_permission)
            .setPositiveButton(R.string.ok, listener)
            .show()
    }

    private fun startCameraSource() {

        val code =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        if (mCameraSource != null) {
            try {
                preview!!.start(mCameraSource, faceOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }
        }
    }

    companion object {
        const val TAG = "MultiTracker"
        private const val RC_HANDLE_GMS = 9001
        const val RC_HANDLE_CAMERA_PERM = 2
    }
}