package com.boringkm.imageclassification.view.main

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import com.boringkm.imageclassification.core.draw.CustomDrawView
import com.boringkm.imageclassification.core.draw.DrawController
import com.boringkm.imageclassification.ui.theme.ImageClassificationTheme
import com.boringkm.imageclassification.view.camera.CameraActivity
import com.boringkm.imageclassification.view.gallery.GalleryActivity

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.i("MainActivity", "PERMISSION GRANTED")
                startActivity(Intent(this@MainActivity, CameraActivity::class.java))
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA),
                    MainViewModel.PERMISSION_CAMERA_CODE
                )
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val drawController = DrawController()

        setContent {
            ImageClassificationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainBody(drawController)
                }
            }
        }
    }

    @Composable
    private fun MainBody(
        drawController: DrawController
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DrawingView(drawController)
            TwoButtons(drawController)
            ResultText()
            Button(
                onClick = {
                    startActivity(Intent(this@MainActivity, GalleryActivity::class.java))
                }
            ) {
                Text("Gallery")
            }
            Button(
                onClick = {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ) {
                Text("Camera")
            }
            Button(
                onClick = {
//                    viewModel.moveToCameraRealTime(applicationContext)
                }
            ) {
                Text("Camera RealTime")
            }
        }
    }

    override fun onDestroy() {
        viewModel.finish()
        super.onDestroy()
    }

    @Composable
    private fun DrawingView(drawController: DrawController) {
        AndroidView(
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .aspectRatio(1f),
            factory = {
                CustomDrawView(it, null, drawController).apply {
                    setStrokeWidth(100.0f)
                    setBackgroundColor(Color.BLACK)
                    setColor(Color.WHITE)
                }
            },
        )
    }

    @Composable
    private fun ResultText() {
        val text: String by viewModel.resultText.observeAsState("")
        Text(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            text = text
        )
    }

    @Composable
    private fun TwoButtons(drawController: DrawController) {
        Row {
            Button(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                onClick = {
                    viewModel.classify(drawController)
                }) {
                Text(text = "Classify")
            }
            Button(
                modifier = Modifier
                    .padding(start = 10.dp)
                    .wrapContentWidth()
                    .wrapContentHeight(),
                onClick = {
                    drawController.clearCanvas()
                }) {
                Text(text = "Clear")
            }
        }
    }
}
