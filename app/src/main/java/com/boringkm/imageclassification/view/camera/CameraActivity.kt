package com.boringkm.imageclassification.view.camera

import android.net.Uri
import android.os.Bundle
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
import androidx.core.content.FileProvider
import com.boringkm.imageclassification.BuildConfig
import com.boringkm.imageclassification.ui.theme.ImageClassificationTheme
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class CameraActivity: ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private var imageUri: Uri? = null
    private val cameraResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
            if (isSuccess.not()) return@registerForActivityResult
            val selectedImage = imageUri ?: return@registerForActivityResult

            viewModel.processResult(selectedImage, contentResolver)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init()

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
                                getTmpFileUri().let { uri ->
                                    imageUri = uri
                                    cameraResult.launch(uri)
                                }
                            }
                        ) {
                            Text("Take Photo")
                        }
                        Text("Result: ${viewModel.resultText.observeAsState().value}")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        viewModel.finish()
        super.onDestroy()
    }

    private fun getTmpFileUri(): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(applicationContext, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }
}