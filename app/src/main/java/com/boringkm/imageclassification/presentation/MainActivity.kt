package com.boringkm.imageclassification.presentation

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.boringkm.imageclassification.ui.theme.ImageClassificationTheme

class MainActivity : ComponentActivity() {
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
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
                        Row {
                            Button(
                                modifier = Modifier
                                    .wrapContentWidth()
                                    .wrapContentHeight(),
                                onClick = {
                                    drawController.getBitmap()
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
                        Text(
                            modifier = Modifier
                                .wrapContentWidth()
                                .wrapContentHeight(),
                            text = "Result"
                        )
                    }
                }
            }
        }
    }
}
