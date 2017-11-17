package com.ramotion.fluidslider

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.OvershootInterpolator


class FluidSlider : View {

    private companion object {
        val BAR_HEIGHT_NORMAL = 56
        val BAR_HEIGHT_SMALL = 40

        val BAR_CORNER_RADIUS = 2
        val BAR_VERTICAL_OFFSET = 1.5f
        val BAR_INNER_HORIZONTAL_OFFSET = 0 // TODO: remove

        val SLIDER_WIDTH = 4
        val SLIDER_HEIGHT = 1 + BAR_VERTICAL_OFFSET

        val TOP_CIRCLE_DIAMETER = 1
        val BOTTOM_CIRCLE_DIAMETER = 3.5f
        val TOUCH_CIRCLE_DIAMETER = 1
        val LABEL_CIRCLE_DIAMETER = 10

        val ANIMATION_DURATION = 400
        val TOP_SPREAD_FACTOR = 0.4
        val BOTTOM_SPREAD_FACTOR = 0.25
        val METABALL_HANDLER_FACTOR = 2.4
        val METABALL_MAX_DISTANCE = 3.0
        val METABALL_RISE_DISTANCE = 1.1f

        val TEXT_SIZE = 12
        val TEXT_OFFSET = 8
        val TEXT_START = "0"
        val TEXT_END = "100"

        val COLOR_BAR = 0xff6168e7.toInt()
        val COLOR_LABEL = Color.WHITE
        val COLOR_LABEL_TEXT = Color.BLACK
        val COLOR_BAR_TEXT = Color.WHITE

        val INITIAL_POSITION = 0.5f
    }

    private val barHeight: Float

    private val desiredWidth: Int
    private val desiredHeight: Int

    private val topCircleDiameter: Float
    private val bottomCircleDiameter: Float
    private val touchRectDiameter: Float
    private val labelRectDiameter: Float

    private val metaballMaxDistance: Double
    private val metaballRiseDistance: Float
    private val textOffset: Float

    private val barVerticalOffset: Float
    private val barCornerRadius: Float
    private val barInnerOffset: Float

    private val rectBar = RectF()
    private val rectTopCircle = RectF()
    private val rectBottomCircle = RectF()
    private val rectTouch = RectF()
    private val rectLabel = RectF()
    private val rectText = Rect()
    private val pathMetaball = Path()

    private val paintBar: Paint
    private val paintLabel: Paint
    private val paintText: Paint

    private var maxMovement = 0f
    private var touchX: Float? = null

    var duration = ANIMATION_DURATION.toLong()
        set(value) { field = Math.abs(value) }

    var colorLabelText = COLOR_LABEL_TEXT
    var colorBarText = COLOR_BAR_TEXT

    var colorBar: Int
        get() = paintBar.color
        set(value) { paintBar.color = value }

    var colorLabel: Int
        get() = paintLabel.color
        set(value) { paintLabel.color = value }

    var textSize: Float
        get() = paintText.textSize
        set(value) { paintText.textSize = value }

    var positionText: String? = null
    var startText: String? = TEXT_START
    var endText: String? = TEXT_END

    var position = INITIAL_POSITION
        set(value) {
            field = Math.max(0f, Math.min(1f, value))
        }

    var positionListener: ((Float) -> Unit)? = null // TODO: check in Java
    var beginTrackingListener: (() -> Unit)? = null
    var endTrackingListener: (() -> Unit)? = null

    @SuppressLint("NewApi")
    inner class OutlineProvider: ViewOutlineProvider() {
        override fun getOutline(v: View?, outline: Outline?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val rect = Rect(rectBar.left.toInt(), rectBar.top.toInt(), rectBar.right.toInt(), rectBar.bottom.toInt())
                outline?.setRoundRect(rect, barCornerRadius)
            }
        }
    }

    class State: BaseSavedState {
        companion object {
            @JvmField @Suppress("unused")
            val CREATOR = object : Parcelable.Creator<State> {
                override fun createFromParcel(parcel: Parcel): State = State(parcel)
                override fun newArray(size: Int): Array<State?> = arrayOfNulls(size)
            }
        }

        val position: Float
        val startText: String?
        val endText: String?
        val textSize: Float
        val colorLabel: Int
        val colorBar: Int
        val colorBarText: Int
        val colorLabelText: Int
        val duration: Long

        constructor(superState: Parcelable,
                            position: Float,
                            startText: String?,
                            endText: String?,
                            textSize: Float,
                            colorLabel: Int,
                            colorBar: Int,
                            colorBarText: Int,
                            colorLabelText: Int,
                            duration: Long) : super(superState)
        {
            this.position = position
            this.startText = startText
            this.endText = endText
            this.textSize = textSize
            this.colorLabel = colorLabel
            this.colorBar = colorBar
            this.colorBarText = colorBarText
            this.colorLabelText = colorLabelText
            this.duration = duration
        }

        private constructor(parcel: Parcel) : super(parcel) {
            this.position = parcel.readFloat()
            this.startText = parcel.readString()
            this.endText = parcel.readString()
            this.textSize = parcel.readFloat()
            this.colorLabel = parcel.readInt()
            this.colorBar = parcel.readInt()
            this.colorBarText = parcel.readInt()
            this.colorLabelText = parcel.readInt()
            this.duration = parcel.readLong()
        }

        override fun writeToParcel(parcel: Parcel, i: Int) {
            parcel.writeFloat(position)
            parcel.writeString(startText)
            parcel.writeString(endText)
            parcel.writeFloat(textSize)
            parcel.writeInt(colorLabel)
            parcel.writeInt(colorBar)
            parcel.writeInt(colorBarText)
            parcel.writeInt(colorLabelText)
            parcel.writeLong(duration)
        }

        override fun describeContents(): Int = 0
    }

    constructor(context: Context) : this(context, null, 0)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outlineProvider = OutlineProvider()
        }

        paintBar = Paint(Paint.ANTI_ALIAS_FLAG)
        paintBar.style = Paint.Style.FILL

        paintLabel = Paint(Paint.ANTI_ALIAS_FLAG)
        paintLabel.style = Paint.Style.FILL

        paintText = Paint(Paint.ANTI_ALIAS_FLAG)

        val density = context.resources.displayMetrics.density

        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(attrs, R.styleable.FluidSlider, defStyleAttr, 0)
            try {
                colorBar = a.getColor(R.styleable.FluidSlider_bar_color, COLOR_BAR)
                colorLabel = a.getColor(R.styleable.FluidSlider_bubble_color, COLOR_LABEL)
                colorBarText = a.getColor(R.styleable.FluidSlider_bar_text_color, COLOR_BAR_TEXT)
                colorLabelText = a.getColor(R.styleable.FluidSlider_bubble_text_color, COLOR_LABEL_TEXT)

                position = Math.max(0f, Math.min(1f, a.getFloat(R.styleable.FluidSlider_initial_position, INITIAL_POSITION)))
                textSize = a.getDimension(R.styleable.FluidSlider_text_size, TEXT_SIZE * density)
                duration = Math.abs(a.getInteger(R.styleable.FluidSlider_duration, ANIMATION_DURATION)).toLong()

                a.getString(R.styleable.FluidSlider_start_text)?.also { startText = it }
                a.getString(R.styleable.FluidSlider_end_text)?.also { endText = it }

                val defaultBarHeight = if (a.getInteger(R.styleable.FluidSlider_size, 1) == 1) BAR_HEIGHT_NORMAL else BAR_HEIGHT_SMALL
                barHeight = defaultBarHeight * density
            } finally {
                a.recycle()
            }
        } else {
            colorBar = COLOR_BAR
            colorLabel = COLOR_LABEL
            textSize = TEXT_SIZE * density
            barHeight = BAR_HEIGHT_NORMAL * density
        }

        desiredWidth = (barHeight * SLIDER_WIDTH).toInt()
        desiredHeight = (barHeight * SLIDER_HEIGHT).toInt()

        topCircleDiameter = barHeight * TOP_CIRCLE_DIAMETER
        bottomCircleDiameter = barHeight * BOTTOM_CIRCLE_DIAMETER
        touchRectDiameter = barHeight * TOUCH_CIRCLE_DIAMETER
        labelRectDiameter = barHeight - LABEL_CIRCLE_DIAMETER * density

        metaballMaxDistance = barHeight * METABALL_MAX_DISTANCE
        metaballRiseDistance = barHeight * METABALL_RISE_DISTANCE

        barVerticalOffset = barHeight * BAR_VERTICAL_OFFSET
        barCornerRadius = BAR_CORNER_RADIUS * density
        barInnerOffset = BAR_INNER_HORIZONTAL_OFFSET * density
        textOffset = TEXT_OFFSET * density
    }

    override fun onSaveInstanceState(): Parcelable {
        return State(super.onSaveInstanceState(),
                position, startText, endText, textSize,
                colorLabel, colorBar, colorBarText, colorLabelText, duration)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        super.onRestoreInstanceState(state)
        if (state is State) {
            position = state.position
            startText = state.startText
            endText = state.endText
            textSize = state.textSize
            colorLabel = state.colorLabel
            colorBar = state.colorBar
            colorBarText = state.colorBarText
            colorLabelText = state.colorLabelText
            duration = state.duration
        }
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

        startText?.let { drawText(canvas, paintText, it, Paint.Align.LEFT, colorBarText, textOffset, rectBar, rectText) }
        endText?.let { drawText(canvas, paintText, it, Paint.Align.RIGHT, colorBarText, textOffset, rectBar, rectText) }

        // Draw metaball
        val x = barInnerOffset + touchRectDiameter / 2 + maxMovement * position
        offsetRectToPosition(x, rectTouch, rectTopCircle, rectBottomCircle, rectLabel)

        drawMetaball(canvas, paintBar,
                pathMetaball, rectBottomCircle, rectTopCircle,
                barInnerOffset, rectBar.right - barInnerOffset, rectBar.top)

        // Draw label and text
        canvas.drawOval(rectLabel, paintLabel)

        val text = positionText ?: (position * 100).toInt().toString()
        drawText(canvas, paintText, text, Paint.Align.CENTER, colorLabelText, 0f, rectLabel, rectText)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            val x = event.x
            val y = event.y
            if (rectBar.contains(x, y)) {
                if (!rectTouch.contains(x, y)) {
                    position = Math.max(0f, Math.min(1f, (x - rectTouch.width() / 2) / maxMovement))
                }
                touchX = x
                beginTrackingListener?.invoke()
                showLabel(metaballRiseDistance)
                true
            } else {
                false
            }
        }
        MotionEvent.ACTION_MOVE -> {
            touchX?.let {
                touchX = event.x
                val newPos = Math.max(0f, Math.min(1f, position + (touchX!! - it) / maxMovement))
                if (newPos != position) positionListener?.invoke(position)
                position = newPos
                invalidate()
                true
            } == true
        }
        MotionEvent.ACTION_UP -> {
            touchX?.let {
                touchX = null
                endTrackingListener?.invoke()
                hideLabel()
                performClick()
                true
            } == true
        }
        else -> false
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
                             leftBorder: Float, rightBorder: Float, topBorder: Float,
                             riseDistance: Float = metaballRiseDistance,
                             maxDistance: Double = metaballMaxDistance,
                             topSpreadFactor: Double = TOP_SPREAD_FACTOR,
                             bottomSpreadFactor: Double = BOTTOM_SPREAD_FACTOR,
                             handleRate: Double = METABALL_HANDLER_FACTOR)
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
        val angle1a = angle1 + u1 + (angle2 - u1) * bottomSpreadFactor
        val angle1b = angle1 - u1 - (angle2 - u1) * bottomSpreadFactor
        val angle2a = (angle1 + Math.PI - u2 - (Math.PI - u2 - angle2) * topSpreadFactor)
        val angle2b = (angle1 - Math.PI + u2 + (Math.PI - u2 - angle2) * topSpreadFactor)

        val p1a = getVector(angle1a, radius1).let { (it.first + circle1.centerX()) to (it.second + circle1.centerY()) }.toList()
        val p1b = getVector(angle1b, radius1).let { (it.first + circle1.centerX()) to (it.second + circle1.centerY()) }.toList()
        val p2a = getVector(angle2a, radius2).let { (it.first + circle2.centerX()) to (it.second + circle2.centerY()) }.toList()
        val p2b = getVector(angle2b, radius2).let { (it.first + circle2.centerX()) to (it.second + circle2.centerY()) }.toList()

        val totalRadius = (radius1 + radius2)
        val d2Base = Math.min(Math.max(topSpreadFactor, bottomSpreadFactor) * handleRate, getVectorLength(p1a[0] to p1a[1], p2a[0] to p2a[1]) / totalRadius)

        // case circles are overlapping:
        val d2 = d2Base * Math.min(1.0, d * 2 / (radius1 + radius2))

        val r1 = radius1 * d2
        val r2 = radius2 * d2

        val pi2 = Math.PI / 2
        val sp1 = getVector(angle1a - pi2, r1).toList().map { it.toFloat() }
        val sp2 = getVector(angle2a + pi2, r2).toList().map { it.toFloat() }
        val sp3 = getVector(angle2b - pi2, r2).toList().map { it.toFloat() }
        val sp4 = getVector(angle1b + pi2, r1).toList().map { it.toFloat() }

        // move bottom point to bar top border
        val yOffsetRatio = Math.min(1f, Math.max(0f,topBorder - circle2.top) /riseDistance)
        val yOffset = (Math.abs(topBorder - p1a[1]) * yOffsetRatio).toFloat() - 1

        val fp1a = p1a.map { it.toFloat() }.let { l -> listOf(Math.min(rightBorder, l[0]), l[1] - yOffset) }
        val fp1b = p1b.map { it.toFloat() }.let { l -> listOf(Math.max(leftBorder, l[0]), l[1] - yOffset) }
        val fp2a = p2a.map { it.toFloat() }
        val fp2b = p2b.map { it.toFloat() }

        path.reset()
        path.moveTo(fp1a[0], fp1a[1])
        path.cubicTo(fp1a[0] + sp1[0], fp1a[1] + sp1[1], fp2a[0] + sp2[0], fp2a[1] + sp2[1], fp2a[0], fp2a[1])
        path.lineTo(fp2b[0], fp2b[1])
        path.cubicTo(fp2b[0] + sp3[0], fp2b[1] + sp3[1], fp1b[0] + sp4[0], fp1b[1] + sp4[1], fp1b[0], fp1b[1])
        path.lineTo(fp1a[0], fp1a[1])
        path.close()

        canvas.drawPath(path, paint)
        canvas.drawOval(circle2, paint)
    }

    private fun drawText(canvas: Canvas, paint: Paint,
                         text: String, align: Paint.Align, color: Int, offset: Float,
                         holderRect: RectF, textRect: Rect)
    {
        paint.color = color
        paint.textAlign = align
        paint.getTextBounds(text, 0, text.length, textRect)
        val x = when (align) {
            Paint.Align.LEFT -> offset
            Paint.Align.CENTER -> holderRect.centerX()
            Paint.Align.RIGHT -> holderRect.right - offset
        }
        val y = holderRect.centerY() + textRect.height() / 2f - textRect.bottom
        canvas.drawText(text, 0, text.length, x, y, paint)
    }

    private fun showLabel(dinstace: Float) {
        val top = barVerticalOffset - dinstace
        val labelVOffset =(topCircleDiameter - labelRectDiameter) / 2f

        val animation = ValueAnimator.ofFloat(rectTopCircle.top, top)
        animation.addUpdateListener {
            rectTopCircle.offsetTo(rectTopCircle.left, it.animatedValue as Float)
            rectLabel.offsetTo(rectLabel.left, it.animatedValue as Float + labelVOffset)
            invalidate()
        }
        animation.duration = duration
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
        animation.duration = duration
        animation.start()
    }

}
