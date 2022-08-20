package com.boringkm.imageclassification.view.realtime

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.media.Image
import android.renderscript.*


object YuvToRgbConverter {
    fun yuvToRgb(context: Context?, image: Image, output: Bitmap?) {
        // FIXME https://developer.android.com/guide/topics/renderscript/migrate
        val rs = RenderScript.create(context)
        val scriptYuvToRgb = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
        val pixelCount = image.cropRect.width() * image.cropRect.height()
        val pixelSizeBits = ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)
        val yuvBuffer = ByteArray(pixelCount * pixelSizeBits / 8)
        imageToByteArray(image, yuvBuffer, pixelCount)
        val elemType = Type.Builder(rs, Element.YUV(rs))
            .setYuvFormat(ImageFormat.NV21)
            .create()
        val inputAllocation = Allocation.createSized(rs, elemType.element, yuvBuffer.size)
        val outputAllocation = Allocation.createFromBitmap(rs, output)
        inputAllocation.copyFrom(yuvBuffer)
        scriptYuvToRgb.setInput(inputAllocation)
        scriptYuvToRgb.forEach(outputAllocation)
        outputAllocation.copyTo(output)
    }

    private fun imageToByteArray(image: Image, outputBuffer: ByteArray, pixelCount: Int) {
        assert(image.format == ImageFormat.YUV_420_888)
        val imageCrop = image.cropRect
        val imagePlanes = image.planes
        for (planeIndex in imagePlanes.indices) {
            val plane = imagePlanes[planeIndex]
            var outputStride: Int
            var outputOffset: Int
            when (planeIndex) {
                0 -> {
                    outputStride = 1
                    outputOffset = 0
                }
                1 -> {
                    outputStride = 2
                    outputOffset = pixelCount + 1
                }
                2 -> {
                    outputStride = 2
                    outputOffset = pixelCount
                }
                else -> {
                    return
                }
            }
            val planeBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            var planeCrop: Rect
            planeCrop = if (planeIndex == 0) {
                imageCrop
            } else {
                Rect(
                    imageCrop.left / 2,
                    imageCrop.top / 2,
                    imageCrop.right / 2,
                    imageCrop.bottom / 2
                )
            }
            val planeWidth = planeCrop.width()
            val planeHeight = planeCrop.height()
            val rowBuffer = ByteArray(plane.rowStride)
            var rowLength: Int
            rowLength = if (pixelStride == 1 && outputStride == 1) {
                planeWidth
            } else {
                (planeWidth - 1) * pixelStride + 1
            }
            for (row in 0 until planeHeight) {
                planeBuffer.position(
                    (row + planeCrop.top) * rowStride + planeCrop.left * pixelStride
                )
                if (pixelStride == 1 && outputStride == 1) {
                    planeBuffer[outputBuffer, outputOffset, rowLength]
                    outputOffset += rowLength
                } else {
                    planeBuffer[rowBuffer, 0, rowLength]
                    for (col in 0 until planeWidth) {
                        outputBuffer[outputOffset] = rowBuffer[col * pixelStride]
                        outputOffset += outputStride
                    }
                }
            }
        }
    }
}