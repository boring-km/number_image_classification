package com.boringkm.imageclassification.presentation

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.boringkm.imageclassification.core.draw.DrawController
import com.boringkm.imageclassification.tflite.Classifier
import java.io.IOException
import java.util.*
import javax.inject.Inject


class MainViewModel @Inject constructor(application: Application): AndroidViewModel(application) {

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

}
