package com.ramotion.fluidslider

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
        val BAR_HEIGHT = 56
        val BAR_CORNER_RADIUS = 10
        val BAR_VERTICAL_OFFSET = 1.5f

        val SLIDER_HEIGHT = BAR_HEIGHT * 3
        val SLIDER_WIDTH = BAR_HEIGHT * 4

        val ANIMATION_DURATION = 1000L

        val METABALL_MAX_DISTANCE = BAR_HEIGHT * 3f
        val METABALL_SCALE_RATE = 1f
        val METABALL_SPREAD_FACTOR = 0.5f
        val METABALL_HANDLER_FACTOR = 0f
        val METABALL_STROKE_WIDTH = 2

        val TOP_CIRCLE_DIAMETER = 56
        val BOTTOM_CIRCLE_DIAMETER = 56*3.5f
        val TOUCH_CIRCLE_DIAMETER = 56
    }

    private val density: Float = context.resources.displayMetrics.density

    private val desiredWidth = (SLIDER_WIDTH * density).toInt()
    private val desiredHeight = (SLIDER_HEIGHT * density).toInt()

    private val topCircleDiameter = TOP_CIRCLE_DIAMETER * density
    private val bottomCircleDiameter = BOTTOM_CIRCLE_DIAMETER * density
    private val touchRectDiameter = TOUCH_CIRCLE_DIAMETER * density

    private val strokeWidth = METABALL_STROKE_WIDTH * density
    private val metaballMaxDistance = METABALL_MAX_DISTANCE * density

    private val barHeight = BAR_HEIGHT * density
    private val barVerticalOffset = barHeight * BAR_VERTICAL_OFFSET
    private val barCornerRadius = BAR_CORNER_RADIUS * density

    private val colorBar = Color.BLUE
    private val colorCircle = Color.WHITE
    private val colorCircleStroke = Color.RED

    // TODO: rename to rect???
    private val barRect = RectF()
    private val topCircleRect = RectF()
    private val bottomCircleRect = RectF()
    private val touchRect = RectF()
    private val metaballPath = Path()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var progress = 0.5f
    private var touchX: Float? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = View.resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val h = View.resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)

        barRect.set(0f, barVerticalOffset, w.toFloat(), barVerticalOffset + barHeight)
        topCircleRect.set(0f, barVerticalOffset, topCircleDiameter, barVerticalOffset + topCircleDiameter)
        bottomCircleRect.set(0f, barVerticalOffset, bottomCircleDiameter, barVerticalOffset + bottomCircleDiameter)
        touchRect.set(0f, barVerticalOffset, touchRectDiameter, barVerticalOffset + touchRectDiameter)

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.color = colorBar
        canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, paint)

        val position = width * progress

        offsetRectToPosition(topCircleRect, position)
        offsetRectToPosition(bottomCircleRect, position)
        offsetRectToPosition(touchRect, position)

        paint.style = Paint.Style.FILL
        paint.color = colorCircle
        canvas.drawOval(topCircleRect, paint)
        canvas.drawOval(bottomCircleRect, paint)
        canvas.drawOval(touchRect, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = colorCircleStroke
        canvas.drawOval(topCircleRect, paint)
        canvas.drawOval(bottomCircleRect, paint)
        canvas.drawOval(touchRect, paint)

        drawMetaball(canvas, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.rawX
                if (touchRect.contains(x, touchRect.top)) {
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
                    val maxMovement = strokeWidth + (width - touchRectDiameter) // buttonStrokeWidth + (width - buttonSize)
                    progress = Math.max(0f, Math.min(1f, progress + (x - it) / maxMovement))
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

    private fun offsetRectToPosition(rect: RectF, position: Float) {
        rect.offsetTo(position - rect.width() / 2f, rect.top)
    }

    private fun drawMetaball(canvas: Canvas, paint: Paint) {
        val d = Math.abs(topCircleRect.centerY() - bottomCircleRect.centerY());

        val radius1 = topCircleRect.width() / 2
        val radius2 = if (d <= metaballMaxDistance) {
            (bottomCircleRect.width() / 2) * METABALL_SCALE_RATE
        } else {
            bottomCircleRect.width() / 2
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

        val center1 = arrayOf(topCircleRect.centerX(), topCircleRect.centerY())
        val center2 = arrayOf(bottomCircleRect.centerX(), bottomCircleRect.centerY())
        val centerDiffX = (center2[0] - center1[0]).toDouble()
        val centerDiffY = (center2[1] - center1[1]).toDouble()

        val angleBetweenCenters = Math.atan2(centerDiffY, centerDiffX).toFloat()
        val maxSpread = Math.acos(((radius1 - radius2) / d).toDouble())

        val angle1a = (angleBetweenCenters + u1 + (maxSpread - u1) * METABALL_SPREAD_FACTOR).toFloat()
        val angle1b = (angleBetweenCenters - u1 - (maxSpread - u1) * METABALL_SPREAD_FACTOR).toFloat()
        val angle2a = (angleBetweenCenters + Math.PI - u2 - (Math.PI - u2 - maxSpread) * METABALL_SPREAD_FACTOR).toFloat()
        val angle2b = (angleBetweenCenters - Math.PI + u2 + (Math.PI - u2 - maxSpread) * METABALL_SPREAD_FACTOR).toFloat()

        val p1a = getVector(angle1a, radius1).zip(center1).map { p -> p.first + p.second }
        val p1b = getVector(angle1b, radius1).zip(center1).map { p -> p.first + p.second }
        val p2a = getVector(angle2a, radius2).zip(center2).map { p -> p.first + p.second }
        val p2b = getVector(angle2b, radius2).zip(center2).map { p -> p.first + p.second }

        val totalRadius = radius1 + radius2
        val dBase = Math.min(METABALL_SPREAD_FACTOR * METABALL_HANDLER_FACTOR,
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

        paint.style = Paint.Style.FILL
        paint.color = colorCircle
        canvas.drawPath(metaballPath, paint);

        paint.style = Paint.Style.STROKE
        paint.color = colorCircleStroke
        canvas.drawPath(metaballPath, paint);
    }

    private fun getLength(x: Float, y: Float): Float {
        return Math.sqrt(x * x.toDouble() + y * y.toDouble()).toFloat()
    }

    private fun getVector(radians: Float, length: Float): Array<Float> {
        val x = (Math.cos(radians.toDouble()) * length).toFloat()
        val y = (Math.sin(radians.toDouble()) * length).toFloat()
        return arrayOf(x, y)
    }

    // TODO: add distance
    private fun showLabel() {
        val animation = ValueAnimator.ofFloat(topCircleRect.top, strokeWidth)
        animation.addUpdateListener {
            topCircleRect.offsetTo(topCircleRect.left, it.animatedValue as Float)
            invalidate()
        }
        animation.duration = ANIMATION_DURATION
        animation.interpolator = OvershootInterpolator()
        animation.start()
    }

    private fun hideLabel() {
        val animation = ValueAnimator.ofFloat(topCircleRect.top, barVerticalOffset)
        animation.duration = ANIMATION_DURATION
        animation.addUpdateListener {
            topCircleRect.offsetTo(topCircleRect.left, it.animatedValue as Float)
            invalidate()
        }
        animation.start()
    }

}
