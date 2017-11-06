package com.ramotion.fluidslider

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator


class FluidSlider : View {

    private companion object {
        val DEFAULT_BUTTON_SIZE = 56
        val DEFAULT_BAR_WIDTH = DEFAULT_BUTTON_SIZE * 4
        val DEFAULT_BAR_HEIGHT = DEFAULT_BUTTON_SIZE * 2
        val DEFAULT_BAR_CORNER_RADIUS = 10
        val DEFAULT_OUTER_STROKE_WIDTH = 5
        val DEFAULT_ANIMATION_DURATION = 300L
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
    private val buttonRect = RectF()
    private val labelRect = RectF()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var progress = 0.5f
    private var touchX: Float? = null

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

        buttonRect.set(0f, barRect.top, buttonSize, barRect.top + buttonSize)
        buttonRect.inset(buttonStrokeWidth, buttonStrokeWidth)

        labelRect.set(0f, barRect.top, buttonSize, barRect.top + buttonSize)
        labelRect.inset(buttonStrokeWidth, buttonStrokeWidth)

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBar(canvas)
        drawButton(canvas)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.rawX
                if (buttonRect.contains(x, buttonRect.top)) {
                    touchX = x
                    showLabel()
                    true
                } else {
                    false
                }
            }
            MotionEvent.ACTION_MOVE -> {
                touchX?.let {
                    val x = event.rawX
                    progress = Math.max(0f, Math.min(1f, progress + (x - it) / maxMovement()))
                    touchX = x;
                    invalidate()
                    true
                } ?: false
            }
            MotionEvent.ACTION_UP -> {
                touchX?.let {
                    touchX = null
                    hideLabel()
                    true
                } ?: false
            }
            else -> false
        }
    }

    private fun drawBar(canvas: Canvas) {
        canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, barPaint)
    }

    private fun drawButton(canvas: Canvas) {
        val left = buttonStrokeWidth + (width - buttonSize) * progress

        buttonRect.offsetTo(left, buttonRect.top)
        canvas.drawOval(buttonRect, buttonPaint)
        canvas.drawOval(buttonRect, buttonStrokePaint)

        labelRect.offsetTo(left, labelRect.top)
        canvas.drawOval(labelRect, buttonPaint)
        canvas.drawOval(labelRect, buttonStrokePaint)
    }

    private fun maxMovement() = buttonStrokeWidth + (width - buttonSize)

    private fun showLabel() {
        val animation = ValueAnimator.ofFloat(labelRect.top, buttonStrokeWidth * 2)
        animation.addUpdateListener {
            labelRect.offsetTo(buttonRect.left, it.animatedValue as Float)
            invalidate()
        }
        animation.duration = DEFAULT_ANIMATION_DURATION
        animation.interpolator = OvershootInterpolator()
        animation.start()
    }

    private fun hideLabel() {
        val animation = ValueAnimator.ofFloat(labelRect.top, buttonRect.top)
        animation.duration = DEFAULT_ANIMATION_DURATION
        animation.addUpdateListener {
            labelRect.offsetTo(buttonRect.left, it.animatedValue as Float)
            invalidate()
        }
        animation.start()
    }

}
