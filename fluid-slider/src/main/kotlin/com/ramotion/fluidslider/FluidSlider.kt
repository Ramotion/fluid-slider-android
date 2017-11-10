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
        val BAR_CORNER_RADIUS = 15
        val BAR_VERTICAL_OFFSET = 1.5f

        val SLIDER_HEIGHT = BAR_HEIGHT * 3
        val SLIDER_WIDTH = BAR_HEIGHT * 4

        val ANIMATION_DURATION = 400L

        val METABALL_MAX_DISTANCE = BAR_HEIGHT * 5.0
        val METABALL_SPREAD_FACTOR = 0.5
        val METABALL_HANDLER_FACTOR = 2.4
        val METABALL_STROKE_WIDTH = 5

        val TOP_CIRCLE_DIAMETER = 56
        val BOTTOM_CIRCLE_DIAMETER = 56*3.5f
        val TOUCH_CIRCLE_DIAMETER = TOP_CIRCLE_DIAMETER
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

    private var progress = 0.0f
    private var maxMovement = 0f
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

        maxMovement = w - touchRectDiameter

        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        paint.color = colorBar
        canvas.drawRoundRect(barRect, barCornerRadius, barCornerRadius, paint)

        val position = touchRectDiameter / 2 + maxMovement * progress

        offsetRectToPosition(touchRect, position)
        offsetRectToPosition(topCircleRect, position)
        offsetRectToPosition(bottomCircleRect, position)

        drawMetaball(canvas, paint, bottomCircleRect, topCircleRect,
                metaballMaxDistance, METABALL_SPREAD_FACTOR, METABALL_HANDLER_FACTOR)

        paint.style = Paint.Style.FILL
        paint.color = colorCircle
        canvas.drawOval(topCircleRect, paint)
//        canvas.drawOval(bottomCircleRect, paint)
//        canvas.drawOval(touchRect, paint)

        /*
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = strokeWidth
        paint.color = colorCircleStroke
        canvas.drawOval(topCircleRect, paint)
        canvas.drawOval(bottomCircleRect, paint)
        canvas.drawOval(touchRect, paint)
        */
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
        rect.offsetTo(position - rect.width()/ 2f, rect.top)
    }


    private fun getVector(radians: Double, length: Double): Pair<Double, Double> {
        val x = (Math.cos(radians) * length)
        val y = (Math.sin(radians) * length)
        return x to y
    }

    private fun getVectorLength(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Double {
        val x = p1.first - p2.first
        val y = p1.second - p2.second
        return Math.sqrt(x * x + y * y)
    }

    private fun drawMetaball(canvas: Canvas, paint: Paint,
                             circle1: RectF, circle2: RectF,
                             maxDistance: Double, v: Double, handleRate: Double)
    {
        val radius1 = circle1.width() / 2.0
        val radius2 = circle2.width() / 2.0

        if (radius1 == 0.0 || radius2 == 0.0) {
            return
        }

        val d = getVectorLength(
                circle1.centerX().toDouble() to circle1.centerY().toDouble(),
                circle2.centerX().toDouble() to circle2.centerY().toDouble())
        if (d > maxDistance || d <= Math.abs(radius1 - radius2)) {
            return
        }

        val u1: Double
        val u2: Double
        if (d < radius1 + radius2) { // case circles are overlapping
            Log.d("D", "circles are overlapping")
            u1 = Math.acos((radius1 * radius1 + d * d - radius2 * radius2) /
                    (2 * radius1 * d))
            u2 = Math.acos((radius2 * radius2 + d * d - radius1 * radius1) /
                    (2 * radius2 * d))
        } else {
            u1 = 0.0
            u2 = 0.0
        }

//        Log.d("D", "d: $d, u1: $u1, r1: $radius1, r2: $radius2")

        val centerXMin = (circle2.centerX() - circle1.centerX()).toDouble()
        val centerYMin = (circle2.centerY() - circle1.centerY()).toDouble()

        val angle1 = Math.atan2(centerYMin, centerXMin)
        val angle2 = Math.acos((radius1 - radius2) / d)
        val angle1a = angle1 + u1 + (angle2 - u1) * v
        val angle1b = angle1 - u1 - (angle2 - u1) * v
        val angle2a = (angle1 + Math.PI - u2 - (Math.PI - u2 - angle2) * v)
        val angle2b = (angle1 - Math.PI + u2 + (Math.PI - u2 - angle2) * v)

        val p1a = getVector(angle1a, radius1).let { (it.first + circle1.centerX()) to (it.second + circle1.centerY()) }.toList()
        val p1b = getVector(angle1b, radius1).let { (it.first + circle1.centerX()) to (it.second + circle1.centerY()) }.toList()
        val p2a = getVector(angle2a, radius2).let { (it.first + circle2.centerX()) to (it.second + circle2.centerY()) }.toList()
        val p2b = getVector(angle2b, radius2).let { (it.first + circle2.centerX()) to (it.second + circle2.centerY()) }.toList()

        val totalRadius = (radius1 + radius2)
        val d2Base = Math.min(v * handleRate, getVectorLength(p1a[0] to p1a[1], p2a[0] to p2a[1]) / totalRadius);

        // case circles are overlapping:
        val d2 = d2Base * Math.min(1.0, d * 2 / (radius1 + radius2));

        val r1 = radius1 * d2
        val r2 = radius2 * d2

        val pi2 = Math.PI / 2
        val sp1 = getVector(angle1a - pi2, r1).toList().map { it.toFloat() }
        val sp2 = getVector(angle2a + pi2, r2).toList().map { it.toFloat() }
        val sp3 = getVector(angle2b - pi2, r2).toList().map { it.toFloat() }
        val sp4 = getVector(angle1b + pi2, r1).toList().map { it.toFloat() }

        val fp1a = p1a.map { it.toFloat() }
        val fp1b = p1b.map { it.toFloat() }
        val fp2a = p2a.map { it.toFloat() }
        val fp2b = p2b.map { it.toFloat() }

        metaballPath.reset()
        metaballPath.moveTo(fp1a[0], fp1a[1])
        metaballPath.cubicTo(fp1a[0] + sp1[0], fp1a[1] + sp1[1], fp2a[0] + sp2[0], fp2a[1] + sp2[1], fp2a[0], fp2a[1])
        metaballPath.lineTo(fp2b[0], fp2b[1]);
        metaballPath.cubicTo(fp2b[0] + sp3[0], fp2b[1] + sp3[1], fp1b[0] + sp4[0], fp1b[1] + sp4[1], fp1b[0], fp1b[1]);
        metaballPath.lineTo(fp1a[0], fp1a[1]);
        metaballPath.close();

        paint.style = Paint.Style.FILL
        paint.color = colorBar
        canvas.drawPath(metaballPath, paint);

        /*
        paint.style = Paint.Style.STROKE
        paint.color = colorCircleStroke
        canvas.drawPath(metaballPath, paint);
        */
    }

    // TODO: add distance
    private fun showLabel() {
        val top = barVerticalOffset - topCircleDiameter
        val animation = ValueAnimator.ofFloat(topCircleRect.top, top)
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
