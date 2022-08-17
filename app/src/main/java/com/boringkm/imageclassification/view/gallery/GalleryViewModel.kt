package com.boringkm.imageclassification.view.gallery

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GalleryViewModel: ViewModel() {
    var resultText = MutableLiveData("")
    var resultBitmap = MutableLiveData(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
}