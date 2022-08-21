package com.boringkm.imageclassification.view.realtime

import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.boringkm.imageclassification.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RealTimeWithXmlActivity: AppCompatActivity() {

    private val viewModel: RealTimeViewModel by viewModels()
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_realtime)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        textView = findViewById(R.id.textView)
        viewModel.init(this@RealTimeWithXmlActivity)
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

    @Synchronized
    override fun onStart() {
        super.onStart()
    }

    @Synchronized
    override fun onStop() {
        super.onStop()
    }
}