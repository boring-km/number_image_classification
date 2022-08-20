package com.boringkm.imageclassification.view.realtime

import android.Manifest
import android.R
import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boringkm.imageclassification.ImageClassificationApp
import com.boringkm.imageclassification.tflite.ClassifierWithModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.inject.Inject


@HiltViewModel
class RealTimeViewModel @Inject constructor(private val model: ClassifierWithModel, application: Application) : AndroidViewModel(application) {

    var frameLayoutId = MutableLiveData(0)
    private var cls: ClassifierWithModel? = null
    var resultText = MutableLiveData("")
        private set
    private var previewWidth = 0
    private var previewHeight = 0
    private var sensorOrientation = 0

    private var rgbFrameBitmap: Bitmap? = null

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var isProcessingFrame = false

    companion object {
        private const val TAG = "CameraView"
    }

    fun init(activity: ComponentActivity) {
        viewModelScope.launch {
            Log.i(TAG, "CameraView init")
            try {
                // New coroutine that can call suspend functions
                cls = model
                cls!!.init()
                setFragment(activity)
            } catch (exception: IOException) {
                Log.e(TAG, exception.stackTraceToString())
            }
        }
    }


    private fun chooseCamera(): String? {
        val context = getApplication<ImageClassificationApp>().applicationContext
        val manager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val chars = manager.getCameraCharacteristics(cameraId)
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                facing.let {
                    if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                        return cameraId
                    }
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        return null
    }

    fun getScreenOrientation(activity: Activity): Int {
        return when (activity.display?.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    fun processImage(reader: ImageReader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if(rgbFrameBitmap == null) {
            rgbFrameBitmap = Bitmap.createBitmap(
                previewWidth,
                previewHeight,
                Bitmap.Config.ARGB_8888)
        }

        if (isProcessingFrame) {
            return
        }

        isProcessingFrame = true

        val image = reader.acquireLatestImage()
        if (image == null) {
            isProcessingFrame = false
            return
        }

        val context = getApplication<ImageClassificationApp>().applicationContext
        YuvToRgbConverter.yuvToRgb(context, image, rgbFrameBitmap)

        runInBackground {
            if (cls != null && cls!!.isInitialized) {
                val output = cls!!.classify(rgbFrameBitmap!!, sensorOrientation)

                resultText.value = String.format(
                    Locale.ENGLISH,
                    "class : %s, prob : %.2f%%",
                    output.first, output.second * 100
                )
            }
            image.close()
            isProcessingFrame = false
        }
    }

    private fun runInBackground(r: Runnable) {
        handler.let { it!!.post(r) }
    }

    fun setFragment(activity: ComponentActivity) {
        val inputSize: Size = cls!!.getModelInputSize()
        val cameraId = chooseCamera()
        if (inputSize.width > 0 && inputSize.height > 0 && cameraId!!.isNotEmpty()) {
            val fragment = CameraFragment.newInstance(
                object : CameraFragment.ConnectionCallback{
                    override fun onPreviewSizeChosen(size: Size, rotation: Int) {
                        previewWidth = size.width
                        previewHeight = size.height
                        sensorOrientation = rotation - getScreenOrientation(activity)
                    }

                },
                { reader -> processImage(reader) },
                inputSize,
                cameraId
            )
            Log.d(
                TAG, "inputSize : " + cls!!.getModelInputSize() +
                        "sensorOrientation : " + sensorOrientation
            )

            // TODO Compose 안에서 Fragment 띄우기: https://medium.com/mobile-app-development-publication/load-fragments-in-jetpack-compose-beyond-what-google-taught-356a7981268d
//            activity.fragmentManager.beginTransaction().replace(
//                frameLayoutId.value!!, fragment
//            ).commit()
        } else {
            Toast.makeText(activity.applicationContext, "Can't find camera", Toast.LENGTH_SHORT).show()
        }
    }

    fun onResume() {
        handlerThread = HandlerThread("InferenceThread")
        handlerThread!!.start()
        handler = Handler(handlerThread!!.looper)
    }

    fun onPause() {
        handlerThread?.quitSafely()
        try {
            handlerThread?.join()
            handlerThread = null
            handler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
