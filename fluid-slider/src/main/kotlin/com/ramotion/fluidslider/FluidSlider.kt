package com.ramotion.fluidslider

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator


class FluidSlider : View {

    private companion object {
        val DEFAULT_BUTTON_SIZE = 56
        val DEFAULT_BAR_WIDTH = DEFAULT_BUTTON_SIZE * 4
        val DEFAULT_BAR_HEIGHT = DEFAULT_BUTTON_SIZE * 2
        val DEFAULT_BAR_CORNER_RADIUS = 10
        val DEFAULT_OUTER_STROKE_WIDTH = 2
        val DEFAULT_ANIMATION_DURATION = 300L
        val DEFAULT_METABALL_MAX_DISTANCE = DEFAULT_BUTTON_SIZE * 2f
        val DEFAULT_METABALL_SCALE_RATE = 1f
        val DEFAULT_METABALL_SPREAD_FACTOR = 0.7f
        val DEFAULT_METABALL_HANDLER_FACTOR = 2f
    }

    private val density: Float = context.resources.displayMetrics.density

    private val desiredWidth = (DEFAULT_BAR_WIDTH * density).toInt()
    private val desiredHeight = (DEFAULT_BAR_HEIGHT * density).toInt()

    private val buttonSize = DEFAULT_BUTTON_SIZE * density
    private val buttonStrokeWidth = DEFAULT_OUTER_STROKE_WIDTH * density

    private val metaballMaxDistance = DEFAULT_METABALL_MAX_DISTANCE * density
    private val barCornerRadius = DEFAULT_BAR_CORNER_RADIUS * density

    private val barColor = Color.BLUE
    private val buttonColor = Color.WHITE
    private val buttonStrokeColor = Color.MAGENTA

    private val barRect = RectF()
    private val buttonRect = RectF()
    private val labelRect = RectF()
    private val metaballPath = Path()

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
        drawMetaball(canvas)
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

    private fun drawMetaball(canvas: Canvas) {
        val d = Math.abs(labelRect.centerY() - buttonRect.centerY());

        val radius1 = buttonRect.width() / 2
        val radius2 = if (d <= metaballMaxDistance) {
            (labelRect.width() / 2) * DEFAULT_METABALL_SCALE_RATE
        } else {
            labelRect.width() / 2
        }

        Log.d("D", "d: $d, r1: $radius1, r2: $radius2, metaballMaxDistance: $metaballMaxDistance")

        if (radius1 == 0f || radius2 == 0f) {
            return
        }

        if (d > metaballMaxDistance || d < Math.abs(radius1 - radius2)) {
            return
        }

        val (u1, u2) = if (d < radius1 + radius2) {
            val sqrR1 = radius1 * radius1
            val sqrR2 = radius2 * radius2
            val sqrD = d * d
            Pair(Math.acos( ((sqrR1 + sqrD - sqrR2) / (2 * radius1 * d)).toDouble() ).toFloat(),
                Math.acos( ((sqrR2 + sqrD - sqrR1) / (2 * radius2 * d)).toDouble() ).toFloat())
        } else {
            Pair(0f, 0f)
        }

        val center1 = arrayOf(buttonRect.centerX(), buttonRect.centerY())
        val center2 = arrayOf(labelRect.centerX(), labelRect.centerY())
        val centerDiffX = (center2[0] - center1[0]).toDouble()
        val centerDiffY = (center2[1] - center1[1]).toDouble()

        val angleBetweenCenters = Math.atan2(centerDiffY, centerDiffX).toFloat()
        val maxSpread = Math.acos(((radius1 - radius2) / d).toDouble())

        val angle1a = (angleBetweenCenters + u1 + (maxSpread - u1) * DEFAULT_METABALL_SPREAD_FACTOR).toFloat()
        val angle1b = (angleBetweenCenters - u1 - (maxSpread - u1) * DEFAULT_METABALL_SPREAD_FACTOR).toFloat()
        val angle2a = (angleBetweenCenters + Math.PI - u2 - (Math.PI - u2 - maxSpread) * DEFAULT_METABALL_SPREAD_FACTOR).toFloat()
        val angle2b = (angleBetweenCenters - Math.PI + u2 + (Math.PI - u2 - maxSpread) * DEFAULT_METABALL_SPREAD_FACTOR).toFloat()

        val p1a = getVector(angle1a, radius1).zip(center1).map { p -> p.first + p.second }
        val p1b = getVector(angle1b, radius1).zip(center1).map { p -> p.first + p.second }
        val p2a = getVector(angle2a, radius2).zip(center2).map { p -> p.first + p.second }
        val p2b = getVector(angle2b, radius2).zip(center2).map { p -> p.first + p.second }

        val totalRadius = radius1 + radius2
        val dBase = Math.min(DEFAULT_METABALL_SPREAD_FACTOR * DEFAULT_METABALL_HANDLER_FACTOR,
                getLength(p1a[0] - p2a[0], p1a[1] - p2a[1]) / totalRadius)
        val d2 = dBase * Math.min(1f, d * 2 / totalRadius)

        val r1 = radius1 * d2
        val r2 = radius2 * d2

        val pi2 = (Math.PI / 2).toFloat()
        val sp1 = getVector(angle1a - pi2, radius1)
        val sp2 = getVector(angle2a + pi2, radius2)
        val sp3 = getVector(angle2b - pi2, radius2)
        val sp4 = getVector(angle1b + pi2, radius1)

        metaballPath.reset()
        metaballPath.moveTo(p1a[0], p1a[1])
        metaballPath.cubicTo(p1a[0] + sp1[0], p1a[1] + sp1[1], p2a[0] + sp2[0], p2a[1] + sp2[1], p2a[0], p2a[1])
        metaballPath.lineTo(p2b[0], p2b[1]);
        metaballPath.cubicTo(p2b[0] + sp3[0], p2b[1] + sp3[1], p1b[0] + sp4[0], p1b[1] + sp4[1], p1b[0], p1b[1]);
        metaballPath.lineTo(p1a[0], p1a[1]);
        metaballPath.close();
        canvas.drawPath(metaballPath, buttonPaint);
        canvas.drawPath(metaballPath, buttonStrokePaint);
    }

    private fun getLength(x: Float, y: Float): Float {
        return Math.sqrt(x * x.toDouble() + y * y.toDouble()).toFloat()
    }

    private fun getVector(radians: Float, length: Float): Array<Float> {
        val x = (Math.cos(radians.toDouble()) * length).toFloat()
        val y = (Math.sin(radians.toDouble()) * length).toFloat()
        return arrayOf(x, y)
    }

    private fun maxMovement() = buttonStrokeWidth + (width - buttonSize)

    // TODO: add distance
    private fun showLabel() {
        val animation = ValueAnimator.ofFloat(labelRect.top, buttonStrokeWidth)
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
