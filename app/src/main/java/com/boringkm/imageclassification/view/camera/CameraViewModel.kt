package com.boringkm.imageclassification.view.camera

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boringkm.imageclassification.repository.TFModelRepository
import com.boringkm.imageclassification.tflite.ClassifierWithModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(private val repository: TFModelRepository) : ViewModel() {

    private var classifier: ClassifierWithModel? = null
    var resultText = MutableLiveData("")
    var resultBitmap = MutableLiveData(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))

    companion object {
        private const val TAG = "CameraView"
    }

    fun init() {
        viewModelScope.launch {
            Log.i(TAG, "CameraView init")
            try {
                // New coroutine that can call suspend functions
                classifier = repository.getModel()
                classifier!!.init()
            } catch (exception: IOException) {
                Log.e(TAG, exception.stackTraceToString())
            }
        }
    }

    fun processResult(selectedImage: Uri, contentResolver: ContentResolver) {
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
}