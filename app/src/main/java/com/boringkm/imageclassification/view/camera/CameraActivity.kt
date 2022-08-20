package com.boringkm.imageclassification.view.camera

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
import com.boringkm.imageclassification.ui.theme.ImageClassificationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CameraActivity : ComponentActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private val cameraResult =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess: Boolean ->
            if (isSuccess.not()) return@registerForActivityResult
            viewModel.processResult(contentResolver)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(cameraResult)

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
                                viewModel.takePhoto(cameraResult)
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
}