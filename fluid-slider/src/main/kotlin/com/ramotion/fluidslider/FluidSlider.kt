package com.ramotion.fluidslider

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View


class FluidSlider : View {

    private companion object {
        val DEFAULT_BUTTON_SIZE = 56
        val DEFAULT_BAR_WIDTH = DEFAULT_BUTTON_SIZE * 4
        val DEFAULT_BAR_HEIGHT = DEFAULT_BUTTON_SIZE * 2
        val DEFAULT_BAR_CORNER_RADIUS = 10
        val DEFAULT_OUTER_STROKE_WIDTH = 2
    }

    private val density: Float = context.resources.displayMetrics.density
    private val desiredWidth = (DEFAULT_BAR_WIDTH * density).toInt()
    private val desiredHeight = (DEFAULT_BAR_HEIGHT * density).toInt()

    private val barCornerRadius = DEFAULT_BAR_CORNER_RADIUS * density
    private val buttonSize = DEFAULT_BUTTON_SIZE * density
    private val buttonStrokeWidth = DEFAULT_OUTER_STROKE_WIDTH * density

    private val barColor = Color.BLUE
    private val buttonColor = Color.WHITE
    private val buttonStrokeColor = Color.MAGENTA

    private val barRect = RectF()
    private val innerCircleRect = RectF()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var  progress = 0.0f

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        barPaint.style = Paint.Style.FILL
        barPaint.color = barColor

        buttonPaint.style = Paint.Style.FILL
        buttonPaint.color = buttonColor

        buttonStrokePaint.style = Paint.Style.STROKE
        buttonStrokePaint.strokeWidth = buttonStrokeWidth
        buttonStrokePaint.color = buttonStrokeColor
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val h = View.resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)

        // TODO: add shadow offset
        barRect.set(0f, h / 2f, w.toFloat(), h.toFloat())

        innerCircleRect.set(0f, 0f, buttonSize, buttonSize)
        innerCircleRect.inset(buttonStrokeWidth, buttonStrokeWidth)

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBar(canvas)
        drawButton(canvas)
    }

    private fun drawBar(canvas: Canvas) {
        canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
    }

    private fun drawButton(canvas: Canvas) {
        val left = buttonStrokeWidth + (width - buttonSize) * progress
        val top = barRect.top + buttonStrokeWidth
        innerCircleRect.offsetTo(left, top)

        canvas.drawOval(innerCircleRect, buttonPaint)
        canvas.drawOval(innerCircleRect, buttonStrokePaint)
    }

}
