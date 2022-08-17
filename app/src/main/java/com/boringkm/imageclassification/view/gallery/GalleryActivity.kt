package com.boringkm.imageclassification.view.gallery

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.boringkm.imageclassification.tflite.ClassifierWithModel
import com.boringkm.imageclassification.ui.theme.ImageClassificationTheme
import java.io.IOException
import java.util.*

class GalleryActivity: ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()
    private lateinit var classifier: ClassifierWithModel

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { url: Uri? ->
        val selectedImage = url ?: return@registerForActivityResult
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
            Toast.makeText(this, "Can not load image!!", Toast.LENGTH_SHORT).show()
        }
        bitmap?.let {
            val output = classifier.classify(bitmap)
            val resultText =
                String.format(Locale.ENGLISH, "class : %s, prob : %.2f%%", output.first, output.second * 100)
            viewModel.resultBitmap.value = bitmap
            viewModel.resultText.value = resultText
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initClassifier()
        setContent {
            ImageClassificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Image(
                            bitmap = viewModel.resultBitmap.observeAsState().value!!.asImageBitmap(),
                            contentDescription = "image",
                        )
                        Button(
                            onClick = {
                                getContent.launch("image/*")
                            }
                        ) {
                            Text("Select Photo")
                        }
                        Text("Result: ${viewModel.resultText.observeAsState().value}")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        classifier.finish()
        super.onDestroy()
    }


    private fun initClassifier() {
        classifier = ClassifierWithModel(this, ClassifierWithModel.IMAGENET_CLASSIFY_MODEL)
        try {
            classifier.init()
        } catch (exception: IOException) {
            Toast.makeText(this, "Can not init Classifier!!", Toast.LENGTH_SHORT).show()
        }
    }
}