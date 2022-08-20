package com.boringkm.imageclassification.view.realtime

import android.content.Context
import android.util.AttributeSet
import android.view.TextureView
import android.view.View

/**
이미지의 가로세로 비율과 TextureView의 가로세로 비율이 맞지 않으면
카메라를 통해 들어오는 이미지가 왜곡될 수 있으므로 이를 맞추기 위해 사용
 */
class AutoFitTextureView(context: Context, attrs: AttributeSet?, defStyle: Int) :
    TextureView(context, attrs, defStyle) {
    private var ratioWidth = 0
    private var ratioHeight = 0

    constructor(context: Context) : this(context, null) {}
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0) {}

    fun setAspectRatio(width: Int, height: Int) {
        require(!(width < 0 || height < 0)) { "Size cannot be negative." }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width: Int = View.MeasureSpec.getSize(widthMeasureSpec)
        val height: Int = View.MeasureSpec.getSize(heightMeasureSpec)
        if (0 == ratioWidth || 0 == ratioHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }
}