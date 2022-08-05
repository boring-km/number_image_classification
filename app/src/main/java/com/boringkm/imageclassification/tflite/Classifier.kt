package com.boringkm.imageclassification.tflite

import android.content.Context
import android.graphics.Bitmap
import com.boringkm.imageclassification.core.argmax
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class Classifier(
    private val context: Context
) {
    private var interpreter: Interpreter? = null

    private var modelInputWidth = 0
    private var modelInputHeight = 0
    private var modelInputChannel = 0
    private var modelOutputClasses = 0

    @Throws(IOException::class)
    fun init() {
        val model = loadModelFile(MODEL_NAME).apply {
            order(ByteOrder.nativeOrder())
        }
        interpreter = Interpreter(model)
        initModelShape(interpreter!!)
    }

    private fun initModelShape(interpreter: Interpreter) {
        val inputTensor = interpreter.getInputTensor(0)
        val inputShape = inputTensor.shape()
        modelInputChannel = inputShape[0]
        modelInputWidth = inputShape[1]
        modelInputHeight = inputShape[2]

        val outputTensor = interpreter.getOutputTensor(0)
        val outputShape = outputTensor.shape()
        modelOutputClasses = outputShape[1]
    }

    fun classify(image: Bitmap): Pair<Int, Float> {
        val buffer = convertBitmapToGrayByteBuffer(resizeBitmap(image))
        val result = Array(1) { FloatArray(modelOutputClasses) }
        interpreter?.run(buffer, result)
        return argmax(result[0])
    }

    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        return Bitmap.createScaledBitmap(bitmap, modelInputWidth, modelInputHeight, false)
    }

    private fun convertBitmapToGrayByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(bitmap.byteCount)
        byteBuffer.order(ByteOrder.nativeOrder())
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixel in pixels) {
            val r = pixel.shr(16).and(0xFF)
            val g = pixel.shr(8).and(0xFF)
            val b = pixel.and(0xFF)

            val avgPixelValue = (r + g + b) / 3.0f
            val normalizedPixelValue = avgPixelValue / 255.0f

            byteBuffer.putFloat(normalizedPixelValue)
        }
        return byteBuffer
    }

    @Throws(IOException::class)
    private fun loadModelFile(modelName: String): ByteBuffer {
        val am = context.assets
        val afd = am.openFd(modelName)
        val fis = FileInputStream(afd.fileDescriptor)
        val fc = fis.channel
        val startOffset = afd.startOffset
        val declaredLength = afd.declaredLength

        return fc.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun finish() {
        if (interpreter != null) {
            interpreter!!.close()
        }
    }

    companion object {
        private const val MODEL_NAME = "keras_model_cnn.tflite"
    }
}
