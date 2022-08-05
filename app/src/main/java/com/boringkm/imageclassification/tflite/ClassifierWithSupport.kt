package com.boringkm.imageclassification.tflite

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import com.boringkm.imageclassification.core.argmax
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.internal.SupportPreconditions
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ClassifierWithSupport(
    private val context: Context
) {
    private lateinit var inputImage: TensorImage
    private var interpreter: Interpreter? = null
    private var modelInputWidth = 0
    private var modelInputHeight = 0
    private var modelInputChannel = 0
    private lateinit var outputBuffer: TensorBuffer
    private var labels: List<String> = ArrayList()

    @Throws(IOException::class)
    fun init() {
        val model = FileUtil.loadMappedFile(context, MODEL_NAME)
        model.order(ByteOrder.nativeOrder())
        interpreter = Interpreter(model)
        initModelShape()
        labels = FileUtil.loadLabels(context, LABEL_FILE)
    }

    fun classify(image: Bitmap): Pair<String, Float> {
        inputImage = loadImage(image)
        interpreter?.run(inputImage.buffer, outputBuffer.buffer.rewind())
        val output = TensorLabel(labels, outputBuffer).mapWithFloatValue
        return argmax(output)
    }

    private fun initModelShape() {
        val inputTensor = interpreter!!.getInputTensor(0)
        val shape = inputTensor.shape()
        modelInputChannel = shape[0]
        modelInputWidth = shape[1]
        modelInputHeight = shape[2]
        inputImage = TensorImage(inputTensor.dataType())
        val outputTensor = interpreter!!.getOutputTensor(0)
        outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
    }

    private fun loadImage(bitmap: Bitmap): TensorImage {
        inputImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(modelInputWidth, modelInputHeight, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .build()
        return imageProcessor.process(inputImage)
    }

    @Throws(IOException::class)
    fun loadMappedFile(context: Context, filePath: String): MappedByteBuffer {
        SupportPreconditions.checkNotNull(context, "Context should not be null.")
        SupportPreconditions.checkNotNull(filePath, "File path cannot be null.")
        val fileDescriptor: AssetFileDescriptor = context.assets.openFd(filePath)

        val var9: MappedByteBuffer

        try {
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)

            try {
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                var9 = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            } catch (var12: Throwable) {
                try {
                    inputStream.close()
                } catch (var11: Throwable) {
                    var12.addSuppressed(var11)
                }
                throw var12
            }
            inputStream.close()
        } catch (var13: Throwable) {
            try {
                fileDescriptor.close()
            } catch (var10: Throwable) {
                var13.addSuppressed(var10)
            }
            throw var13
        }
        fileDescriptor.close()
        return var9
    }

    fun finish() {
        interpreter!!.close()
    }

    companion object {
        private const val MODEL_NAME = "mobilenet_imagenet_model.tflite"
        private const val LABEL_FILE = "labels.txt"
    }
}