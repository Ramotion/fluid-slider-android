package com.ramotion.fluidslider

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator


// TODO: implement status saving (position)
class FluidSlider : View {

    private companion object {
        val BAR_HEIGHT = 56
        val BAR_CORNER_RADIUS = 15
        val BAR_VERTICAL_OFFSET = 1.5f
        val BAR_INNER_HORIZONTAL_OFFSET = 4

        val SLIDER_HEIGHT = BAR_HEIGHT * 3
        val SLIDER_WIDTH = BAR_HEIGHT * 4

        val ANIMATION_DURATION = 500L

        val METABALL_MAX_DISTANCE = BAR_HEIGHT * 5.0
        val METABALL_SPREAD_FACTOR = 0.25
        val METABALL_HANDLER_FACTOR = 2.4

        val TOP_CIRCLE_DIAMETER = 56
        val BOTTOM_CIRCLE_DIAMETER = 56 * 3.5f
        val TOUCH_CIRCLE_DIAMETER = TOP_CIRCLE_DIAMETER
        val LABEL_CIRCLE_DIAMETER = 46

        val TEXT_SIZE = 12
        val TEXT_LEFT = "0"
        val TEXT_RIGHT = "100"
    }

    private val density: Float = context.resources.displayMetrics.density

    private val desiredWidth = (SLIDER_WIDTH * density).toInt()
    private val desiredHeight = (SLIDER_HEIGHT * density).toInt()

    private val topCircleDiameter = TOP_CIRCLE_DIAMETER * density
    private val bottomCircleDiameter = BOTTOM_CIRCLE_DIAMETER * density
    private val touchRectDiameter = TOUCH_CIRCLE_DIAMETER * density
    private val labelRectDiameter = LABEL_CIRCLE_DIAMETER * density

    private val metaballMaxDistance = METABALL_MAX_DISTANCE * density

    private val textSize = TEXT_SIZE * density
    private val textLeft = TEXT_LEFT
    private val textRight = TEXT_RIGHT

    private val barHeight = BAR_HEIGHT * density
    private val barVerticalOffset = barHeight * BAR_VERTICAL_OFFSET
    private val barCornerRadius = BAR_CORNER_RADIUS * density
    private val barInnerOffset = BAR_INNER_HORIZONTAL_OFFSET * density

    private val colorBar = 0xff6168e7.toInt()
    private val colorLabel = Color.WHITE
    private val colorLabelText = Color.BLACK
    private val colorBarText = Color.WHITE

    private val rectBar = RectF()
    private val rectTopCircle = RectF()
    private val rectBottomCircle = RectF()
    private val rectTouch = RectF()
    private val rectLabel = RectF()
    private val rectText = Rect()
    private val pathMetaball = Path()

    private val paintBar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintLabel = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var progress = 0.5f
    private var maxMovement = 0f
    private var touchX: Float? = null

    inner class OutlineProvider: ViewOutlineProvider() {
        override fun getOutline(v: View?, outline: Outline?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val rect = Rect(rectBar.left.toInt(), rectBar.top.toInt(), rectBar.right.toInt(), rectBar.bottom.toInt())
                outline?.setRoundRect(rect, barCornerRadius)
            }
        }
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = OutlineProvider()
        }

        paintBar.style = Paint.Style.FILL
        paintBar.color = colorBar

        paintLabel.style = Paint.Style.FILL
        paintLabel.color = colorLabel

        paintText.textSize = textSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSizeAndState(desiredWidth, widthMeasureSpec, 0)
        val h = resolveSizeAndState(desiredHeight, heightMeasureSpec, 0)

        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        val width = w.toFloat()

        rectBar.set(0f, barVerticalOffset, width, barVerticalOffset + barHeight)
        rectTopCircle.set(0f, barVerticalOffset, topCircleDiameter, barVerticalOffset + topCircleDiameter)
        rectBottomCircle.set(0f, barVerticalOffset, bottomCircleDiameter, barVerticalOffset + bottomCircleDiameter)
        rectTouch.set(0f, barVerticalOffset, touchRectDiameter, barVerticalOffset + touchRectDiameter)

        val vOffset = barVerticalOffset + (topCircleDiameter - labelRectDiameter) / 2f
        rectLabel.set(0f, vOffset, labelRectDiameter, vOffset + labelRectDiameter)

        maxMovement = width - touchRectDiameter - barInnerOffset * 2
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw slider bar and text
        canvas.drawRoundRect(rectBar, barCornerRadius, barCornerRadius, paintBar)

        paintText.color = colorBarText
        paintText.textAlign = Paint.Align.LEFT
        paintText.getTextBounds(textLeft, 0, textLeft.length, rectText)
        val barTextLeftX = barCornerRadius
        val barTextLeftY = rectBar.centerY() + rectText.height() / 2f - rectText.bottom
        canvas.drawText(textLeft, 0, textLeft.length, barTextLeftX, barTextLeftY, paintText)

        paintText.textAlign = Paint.Align.RIGHT
        val barTextRightX = rectBar.right - barCornerRadius
        canvas.drawText(textRight, 0, textRight.length, barTextRightX, barTextLeftY, paintText)

        // Draw metaball
        val position = barInnerOffset + touchRectDiameter / 2 + maxMovement * progress
        offsetRectToPosition(position, rectTouch, rectTopCircle, rectBottomCircle, rectLabel)

        drawMetaball(canvas, paintBar,
                pathMetaball, rectBottomCircle, rectTopCircle,
                metaballMaxDistance, METABALL_SPREAD_FACTOR, METABALL_HANDLER_FACTOR)

        // Draw label and text
        canvas.drawOval(rectLabel, paintLabel)

        val textLabel = (progress * 100).toInt().toString() // TODO: get label text from listener
        paintText.color = colorLabelText
        paintText.textAlign = Paint.Align.CENTER
        paintText.getTextBounds(textLabel, 0, textLabel.length, rectText)
        val labelTextX = rectLabel.centerX();
        val labelTextY = rectLabel.centerY() + rectText.height() / 2f - rectText.bottom
        canvas.drawText(textLabel, 0, textLabel.length, labelTextX , labelTextY, paintText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.rawX
                if (rectTouch.contains(x, rectTouch.top)) {
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

    private fun offsetRectToPosition(position: Float, vararg rects: RectF) {
        for (rect in rects) {
            rect.offsetTo(position - rect.width()/ 2f, rect.top)
        }
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
                             path: Path, circle1: RectF, circle2: RectF,
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
            u1 = Math.acos((radius1 * radius1 + d * d - radius2 * radius2) /
                    (2 * radius1 * d))
            u2 = Math.acos((radius2 * radius2 + d * d - radius1 * radius1) /
                    (2 * radius2 * d))
        } else {
            u1 = 0.0
            u2 = 0.0
        }

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

        val fp1a = p1a.map { it.toFloat() }.let { l -> listOf(Math.min(rectBar.right - barInnerOffset, l[0]), l[1]) }
        val fp1b = p1b.map { it.toFloat() }.let { l -> listOf(Math.max(barInnerOffset, l[0]), l[1]) }
        val fp2a = p2a.map { it.toFloat() }
        val fp2b = p2b.map { it.toFloat() }

        path.reset()
        path.moveTo(fp1a[0], fp1a[1])
        path.cubicTo(fp1a[0] + sp1[0], fp1a[1] + sp1[1], fp2a[0] + sp2[0], fp2a[1] + sp2[1], fp2a[0], fp2a[1])
        path.lineTo(fp2b[0], fp2b[1]);
        path.cubicTo(fp2b[0] + sp3[0], fp2b[1] + sp3[1], fp1b[0] + sp4[0], fp1b[1] + sp4[1], fp1b[0], fp1b[1]);
        path.lineTo(fp1a[0], fp1a[1]);
        path.close();

        canvas.drawPath(pathMetaball, paint);
        canvas.drawOval(circle2, paint)
    }

    // TODO: add distance
    private fun showLabel() {
        val top = barVerticalOffset - topCircleDiameter - 10
        val labelVOffset =(topCircleDiameter - labelRectDiameter) / 2f

        val animation = ValueAnimator.ofFloat(rectTopCircle.top, top)
        animation.addUpdateListener {
            rectTopCircle.offsetTo(rectTopCircle.left, it.animatedValue as Float)
            rectLabel.offsetTo(rectLabel.left, it.animatedValue as Float + labelVOffset)
            invalidate()
        }
        animation.duration = ANIMATION_DURATION
        animation.interpolator = OvershootInterpolator()
        animation.start()
    }

    private fun hideLabel() {
        val labelVOffset =(topCircleDiameter - labelRectDiameter) / 2f
        val animation = ValueAnimator.ofFloat(rectTopCircle.top, barVerticalOffset)
        animation.addUpdateListener {
            rectTopCircle.offsetTo(rectTopCircle.left, it.animatedValue as Float)
            rectLabel.offsetTo(rectLabel.left, it.animatedValue as Float + labelVOffset)
            invalidate()
        }
        animation.duration = ANIMATION_DURATION
        animation.interpolator = AnticipateInterpolator()
        animation.start()
    }

}
