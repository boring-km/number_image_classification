package com.boringkm.imageclassification.view.camera

import android.app.Application
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.boringkm.imageclassification.BuildConfig
import com.boringkm.imageclassification.ImageClassificationApp
import com.boringkm.imageclassification.tflite.ClassifierWithModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(private val model: ClassifierWithModel, application: Application) : AndroidViewModel(application) {

    var imageUri: Uri? = null
    private var classifier: ClassifierWithModel? = null
    var resultText = MutableLiveData("")
    var resultBitmap = MutableLiveData(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    companion object {
        private const val TAG = "CameraView"
    }

    fun init(cameraResult: ActivityResultLauncher<Uri>) {
        viewModelScope.launch {
            Log.i(TAG, "CameraView init")
            try {
                // New coroutine that can call suspend functions
                classifier = model
                classifier!!.init()
                takePhoto(cameraResult)
            } catch (exception: IOException) {
                Log.e(TAG, exception.stackTraceToString())
            }
        }
    }

    fun processResult(contentResolver: ContentResolver) {
        val selectedImage = imageUri ?: return
        var bitmap: Bitmap? = null
        try {
            bitmap = if (Build.VERSION.SDK_INT >= 28) {
                val src = ImageDecoder.createSource(contentResolver, selectedImage)
                ImageDecoder.decodeBitmap(src)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, selectedImage)
            }
        } catch (exception: IOException) {
            Log.e(TAG, "Can not load image!!")
        }
        bitmap?.let {
            val output = classifier!!.classify(bitmap)

            resultBitmap.value = bitmap
            resultText.value = String.format(
                Locale.ENGLISH,
                "class : %s, prob : %.2f%%",
                output.first,
                output.second * 100
            )
        }
    }

    fun finish() {
        classifier!!.finish()
    }

    fun takePhoto(cameraResult: ActivityResultLauncher<Uri>) {
        val context = getApplication<ImageClassificationApp>().applicationContext
        val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        imageUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.provider",
            tmpFile
        )
        cameraResult.launch(imageUri)
    }
}