package com.boringkm.imageclassification.view.realtime

import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.MutableLiveData
import com.boringkm.imageclassification.R
import com.boringkm.imageclassification.ui.theme.ImageClassificationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RealTimeActivity : ComponentActivity() {

    private val viewModel: RealTimeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(this@RealTimeActivity)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            ImageClassificationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
//                        AndroidView(
//                            modifier = Modifier.fillMaxWidth(),
//                            factory = {
//                                FrameLayout(it)
//                            }, update = {
//
//                                if (viewModel.fragment != null) {
//                                    it.addView(viewModel.fragment!!.value!!.view)
//                                }
////                                if (viewModel.fragment != null) {
////                                    fragmentManager.beginTransaction().replace(
////                                        it.id, viewModel.fragment!!.value
////                                    ).commit()
////                                }
//                            })
                        Text("Result: ${viewModel.resultText.observeAsState().value}")
                    }
                }
            }
        }
    }

    @Synchronized
    override fun onResume() {
        super.onResume()

        viewModel.onResume()
    }

    @Synchronized
    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

}