package com.boringkm.imageclassification.presentation

import android.graphics.Bitmap

class DrawController {
    private var drawView: CustomDrawView? = null

    fun getBitmap(): Bitmap? {
        return drawView?.getBitmap();
    }

    fun clearCanvas() {
        drawView?.clearCanvas();
    }

    fun setBitmap(customDrawView: CustomDrawView) {
        this.drawView = customDrawView;
    }
}