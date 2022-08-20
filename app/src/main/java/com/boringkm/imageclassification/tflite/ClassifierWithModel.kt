package com.boringkm.imageclassification.tflite

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod.NEAREST_NEIGHBOR


class ClassifierWithModel(private var context: Context, private val modelName: String) {

    var isInitialized: Boolean = false
        private set
    private lateinit var model: Model
    private lateinit var inputImage: TensorImage
    private lateinit var outputBuffer: TensorBuffer

    private var modelInputChannel = 0
    private var modelInputWidth = 0
    private var modelInputHeight = 0
    private val labels = mutableListOf<String>()

    fun init() {
        model = Model.createModel(context, modelName)   // modelName으로 모델 생성
        initModelShape()
        labels.addAll(FileUtil.loadLabels(context, LABEL_FILE))
        isInitialized = true
    }

    fun getModelInputSize(): Size {
        if (!isInitialized) {
            return Size(0, 0)
        }
        return Size(modelInputWidth, modelInputHeight)
    }

    private fun initModelShape() {
        val inputTensor = model.getInputTensor(0)   // 인터프리터가 아니라 모델에서 inputTensor 가져옴
        val inputShape = inputTensor.shape()
        modelInputChannel = inputShape[0]
        modelInputWidth = inputShape[1]
        modelInputHeight = inputShape[2]

        inputImage = TensorImage(inputTensor.dataType())

        val outputTensor = model.getOutputTensor(0)
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
    }

    fun classify(image: Bitmap): Pair<String, Float> {
        inputImage = loadImage(image)
        val inputs = arrayOf<Any>(inputImage.buffer)
        val outputs = mutableMapOf<Int, Any>()
        outputs[0] = outputBuffer.buffer.rewind()
        model.run(inputs, outputs)
        val output = TensorLabel(labels, outputBuffer).mapWithFloatValue
        return argmax(output)
    }

    fun classify(image: Bitmap?, sensorOrientation: Int): Pair<String, Float> {
        inputImage = loadImage(image!!, sensorOrientation)
        val inputs = arrayOf<Any>(inputImage.buffer)
        val outputs: MutableMap<Int, Any> = HashMap()
        outputs[0] = outputBuffer.buffer.rewind()
        model.run(inputs, outputs)
        val output = TensorLabel(labels, outputBuffer).mapWithFloatValue
        return argmax(output)
    }

    private fun loadImage(bitmap: Bitmap): TensorImage {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap))
        } else {
            inputImage.load(bitmap)
        }
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        return imageProcessor.process(inputImage)
    }

    private fun loadImage(bitmap: Bitmap, sensorOrientation: Int): TensorImage {
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            inputImage.load(convertBitmapToARGB8888(bitmap))
        } else {
            inputImage.load(bitmap)
        }
        val cropSize = Math.min(bitmap.width, bitmap.height)
        val numRotation = sensorOrientation / 90
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(modelInputWidth, modelInputHeight, NEAREST_NEIGHBOR))
            .add(Rot90Op(numRotation))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()
        return imageProcessor.process(inputImage)
    }

    private fun convertBitmapToARGB8888(bitmap: Bitmap) = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    private fun argmax(map: Map<String, Float>) =
        map.entries.maxByOrNull { it.value }?.let {
            it.key to it.value
        } ?: ("" to 0f)

    fun finish() {
        if (::model.isInitialized) {
            model.close()
            isInitialized = false
        }
    }

    companion object {
        const val IMAGENET_CLASSIFY_MODEL = "mobilenet_imagenet_model.tflite"
        const val LABEL_FILE = "labels.txt"
    }
}