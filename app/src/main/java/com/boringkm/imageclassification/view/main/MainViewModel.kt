package com.boringkm.imageclassification.view.main

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.boringkm.imageclassification.ImageClassificationApp
import com.boringkm.imageclassification.core.draw.DrawController
import com.boringkm.imageclassification.tflite.Classifier
import com.boringkm.imageclassification.view.camera.CameraActivity
import com.boringkm.imageclassification.view.gallery.GalleryActivity
import java.io.IOException
import java.util.*
import javax.inject.Inject


class MainViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {

    private lateinit var cls: Classifier
    private val _resultText = MutableLiveData("")
    val resultText: LiveData<String> = _resultText

    init {
        try {
            cls = Classifier(application.applicationContext)
            cls.init()
        } catch (e: IOException) {
            Log.e("DigitClassifier", "failed to init Classifier", e)
        }
    }

    fun classify(drawController: DrawController) {
        val bitmap = drawController.getBitmap()
        val res = cls.classify(bitmap!!)
        val output = String.format(Locale.ENGLISH, "%s, %.0f%%", res.first, res.second * 100.0f)
        onResultChange(output)
    }

    fun finish() {
        cls.finish()
    }

    private fun onResultChange(result: String) {
        _resultText.value = result
    }

    fun moveWithCameraPermission(permissionLauncher: ActivityResultLauncher<String>) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    companion object {
        const val PERMISSION_CAMERA_CODE = 11
    }

}
