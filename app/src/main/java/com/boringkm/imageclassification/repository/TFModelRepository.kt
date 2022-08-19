package com.boringkm.imageclassification.repository

import com.boringkm.imageclassification.tflite.ClassifierWithModel
import javax.inject.Inject

class TFModelRepository @Inject constructor(private val model: ClassifierWithModel) {
    // suspend가 필요는 없지만 코루틴 연습을 위해 추가해봄
    suspend fun getModel() = model
}
