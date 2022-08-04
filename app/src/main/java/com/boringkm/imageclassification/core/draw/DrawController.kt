package com.boringkm.imageclassification.core.draw

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class DrawController {
    private var drawView: LiveData<CustomDrawView>? = null

    fun getBitmap(): Bitmap? {
        return drawView?.value?.getBitmap()
    }

    fun clearCanvas() {
        drawView?.value?.clearCanvas()
    }

    fun setBitmap(customDrawView: CustomDrawView) {
        this.drawView = MutableLiveData(customDrawView)
    }
}