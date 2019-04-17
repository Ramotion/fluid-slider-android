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
import com.ramotion.fluidslider.FluidSlider.Size.NORMAL
import com.ramotion.fluidslider.FluidSlider.Size.SMALL
import kotlin.math.*


class FluidSlider @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        size: Size = Size.NORMAL) : View(context, attrs, defStyleAttr) {

    /**
     * Sizes that can be used.
     * @see NORMAL
     * @see SMALL
     */
    enum class Size(val value: Int) {
        /**
         * Default size - 56dp.
         */
        NORMAL(56),

        /**
         * Small size - 40dp.
         */
        SMALL(40)
    }

    private companion object {
        const val BAR_CORNER_RADIUS = 2
        const val BAR_VERTICAL_OFFSET = 1.5f
        const val BAR_INNER_HORIZONTAL_OFFSET = 0

        const val SLIDER_WIDTH = 4
        const val SLIDER_HEIGHT = 1 + BAR_VERTICAL_OFFSET

        const val TOP_CIRCLE_DIAMETER = 1
        const val BOTTOM_CIRCLE_DIAMETER = 25.0f
        const val TOUCH_CIRCLE_DIAMETER = 1
        const val LABEL_CIRCLE_DIAMETER = 10

        const val ANIMATION_DURATION = 400
        const val TOP_SPREAD_FACTOR = 0.4f
        const val BOTTOM_START_SPREAD_FACTOR = 0.25f
        const val BOTTOM_END_SPREAD_FACTOR = 0.1f
        const val METABALL_HANDLER_FACTOR = 2.4f
        const val METABALL_MAX_DISTANCE = 15.0f
        const val METABALL_RISE_DISTANCE = 1.1f

        const val TEXT_SIZE = 12
        const val TEXT_OFFSET = 8
        const val TEXT_START = "0"
        const val TEXT_END = "100"

        const val COLOR_BAR = 0xff6168e7.toInt()
        const val COLOR_LABEL = Color.WHITE
        const val COLOR_LABEL_TEXT = Color.BLACK
        const val COLOR_BAR_TEXT = Color.WHITE

        const val INITIAL_POSITION = 0.5f
    }

    private val barHeight: Float

    private val desiredWidth: Int
    private val desiredHeight: Int

    private val topCircleDiameter: Float
    private val bottomCircleDiameter: Float
    private val touchRectDiameter: Float
    private val labelRectDiameter: Float

    private val metaballMaxDistance: Float
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

    /**
     * Duration of "bubble" rise in milliseconds.
     */
    var duration = ANIMATION_DURATION.toLong()
        set(value) {
            field = abs(value)
        }

    /**
     * Color of text inside "bubble".
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var colorBubbleText = COLOR_LABEL_TEXT

    /**
     * Color of `start` and `end` texts of slider.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var colorBarText = COLOR_BAR_TEXT

    /**
     * Color of slider.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var colorBar: Int
        get() = paintBar.color
        set(value) {
            paintBar.color = value
        }

    /**
     * Color of circle "bubble" inside bar.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    var colorBubble: Int
        get() = paintLabel.color
        set(value) {
            paintLabel.color = value
        }

    /**
     * Text size.
     */
    var textSize: Float
        get() = paintText.textSize
        set(value) {
            paintText.textSize = value
        }

    /**
     * Bubble text.
     */
    var bubbleText: String? = null

    /**
     * Start (left) text of slider.
     */
    var startText: String? = TEXT_START

    /**
     * End (right) text of slider.
     */
    var endText: String? = TEXT_END

    /**
     * Initial position of "bubble" in range form `0.0` to `1.0`.
     */
    var position = INITIAL_POSITION
        set(value) {
            field = max(0f, min(1f, value))
            invalidate()
            positionListener?.invoke(field)
        }

    /**
     * Current position tracker. Receive current position, in range from `0.0f` to `1.0f`.
     */
    var positionListener: ((Float) -> Unit)? = null

    /**
     * Called on slider touch.
     */
    var beginTrackingListener: (() -> Unit)? = null

    /**
     * Called when slider is released.
     */
    var endTrackingListener: (() -> Unit)? = null

    @SuppressLint("NewApi")
    inner class OutlineProvider : ViewOutlineProvider() {
        override fun getOutline(v: View?, outline: Outline?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val rect = Rect(rectBar.left.toInt(), rectBar.top.toInt(), rectBar.right.toInt(), rectBar.bottom.toInt())
                outline?.setRoundRect(rect, barCornerRadius)
            }
        }
    }

    class State : BaseSavedState {
        companion object {
            @JvmField
            @Suppress("unused")
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
                    duration: Long) : super(superState) {
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
            super.writeToParcel(parcel, i)
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

    init {
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
                colorBubble = a.getColor(R.styleable.FluidSlider_bubble_color, COLOR_LABEL)
                colorBarText = a.getColor(R.styleable.FluidSlider_bar_text_color, COLOR_BAR_TEXT)
                colorBubbleText = a.getColor(R.styleable.FluidSlider_bubble_text_color, COLOR_LABEL_TEXT)

                position = max(0f, min(1f, a.getFloat(R.styleable.FluidSlider_initial_position, INITIAL_POSITION)))
                textSize = a.getDimension(R.styleable.FluidSlider_text_size, TEXT_SIZE * density)
                duration = abs(a.getInteger(R.styleable.FluidSlider_duration, ANIMATION_DURATION)).toLong()

                a.getString(R.styleable.FluidSlider_start_text)?.also { startText = it }
                a.getString(R.styleable.FluidSlider_end_text)?.also { endText = it }

                val defaultBarHeight = if (a.getInteger(R.styleable.FluidSlider_size, 1) == 1) Size.NORMAL.value else Size.SMALL.value
                barHeight = defaultBarHeight * density
            } finally {
                a.recycle()
            }
        } else {
            colorBar = COLOR_BAR
            colorBubble = COLOR_LABEL
            textSize = TEXT_SIZE * density
            barHeight = size.value * density
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

    /**
     * Additional constructor that can be used to create FluidSlider programmatically.
     * @param context The Context the view is running in, through which it can access the current theme, resources, etc.
     * @param size Size of FluidSlider.
     * @see Size
     */
    constructor(context: Context, size: Size) : this(context, null, 0, size)

    override fun onSaveInstanceState(): Parcelable {
        return State(super.onSaveInstanceState(),
                position, startText, endText, textSize,
                colorBubble, colorBar, colorBarText, colorBubbleText, duration)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is State) {
            super.onRestoreInstanceState(state.superState)
            position = state.position
            startText = state.startText
            endText = state.endText
            textSize = state.textSize
            colorBubble = state.colorLabel
            colorBar = state.colorBar
            colorBarText = state.colorBarText
            colorBubbleText = state.colorLabelText
            duration = state.duration
        } else {
            super.onRestoreInstanceState(state)
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

        drawMetaball(canvas, paintBar, pathMetaball, rectBottomCircle, rectTopCircle, rectBar.top)

        // Draw label and text
        canvas.drawOval(rectLabel, paintLabel)

        val text = bubbleText ?: (position * 100).toInt().toString()
        drawText(canvas, paintText, text, Paint.Align.CENTER, colorBubbleText, 0f, rectLabel, rectText)
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
                    position = max(0f, min(1f, (x - rectTouch.width() / 2) / maxMovement))
                }
                touchX = x
                beginTrackingListener?.invoke()
                showLabel(metaballRiseDistance)
                parent.requestDisallowInterceptTouchEvent(true)
                true
            } else {
                false
            }
        }
        MotionEvent.ACTION_MOVE -> {
            touchX?.let {
                touchX = event.x
                val newPos = max(0f, min(1f, position + (event.x - it) / maxMovement))
                position = newPos
                true
            } == true
        }
        MotionEvent.ACTION_UP,
        MotionEvent.ACTION_CANCEL -> {
            touchX?.let {
                touchX = null
                endTrackingListener?.invoke()
                hideLabel()
                performClick()
                parent.requestDisallowInterceptTouchEvent(false)
                true
            } == true
        }
        else -> false
    }

    private fun offsetRectToPosition(position: Float, vararg rects: RectF) {
        for (rect in rects) {
            rect.offsetTo(position - rect.width() / 2f, rect.top)
        }
    }

    private fun getVector(radians: Float, length: Float): Pair<Float, Float> {
        val x = (cos(radians) * length)
        val y = (sin(radians) * length)
        return x to y
    }

    private fun getVectorLength(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val x = x1 - x2
        val y = y1 - y2
        return sqrt(x * x + y * y)
    }

    private fun drawMetaball(canvas: Canvas,
                             paint: Paint,
                             path: Path,
                             circle1: RectF,
                             circle2: RectF,
                             topBorder: Float,
                             riseDistance: Float = metaballRiseDistance,
                             maxDistance: Float = metaballMaxDistance,
                             cornerRadius: Float = barCornerRadius,
                             topSpreadFactor: Float = TOP_SPREAD_FACTOR,
                             bottomStartSpreadFactor: Float = BOTTOM_START_SPREAD_FACTOR,
                             bottomEndSpreadFactor: Float = BOTTOM_END_SPREAD_FACTOR,
                             handleRate: Float = METABALL_HANDLER_FACTOR) {
        val radius1 = circle1.width() / 2.0f
        val radius2 = circle2.width() / 2.0f

        if (radius1 == 0.0f || radius2 == 0.0f) {
            return
        }

        val d = getVectorLength(circle1.centerX(), circle1.centerY(), circle2.centerX(), circle2.centerY())
        if (d > maxDistance || d <= abs(radius1 - radius2)) {
            return
        }

        val riseRatio = min(1f, max(0f, topBorder - circle2.top) / riseDistance)

        val u1: Float
        val u2: Float
        if (d < radius1 + radius2) { // case circles are overlapping
            u1 = acos((radius1 * radius1 + d * d - radius2 * radius2) / (2 * radius1 * d))
            u2 = acos((radius2 * radius2 + d * d - radius1 * radius1) / (2 * radius2 * d))
        } else {
            u1 = 0.0f
            u2 = 0.0f
        }

        val centerXMin = circle2.centerX() - circle1.centerX()
        val centerYMin = circle2.centerY() - circle1.centerY()

        val bottomSpreadDiff = bottomStartSpreadFactor - bottomEndSpreadFactor
        val bottomSpreadFactor = bottomStartSpreadFactor - bottomSpreadDiff * riseRatio

        val fPI = PI.toFloat()
        val angle1 = atan2(centerYMin, centerXMin)
        val angle2 = acos((radius1 - radius2) / d)
        val angle1a = angle1 + u1 + (angle2 - u1) * bottomSpreadFactor
        val angle1b = angle1 - u1 - (angle2 - u1) * bottomSpreadFactor
        val angle2a = (angle1 + fPI - u2 - (fPI - u2 - angle2) * topSpreadFactor)
        val angle2b = (angle1 - fPI + u2 + (fPI - u2 - angle2) * topSpreadFactor)

        val p1a = getVector(angle1a, radius1).let { (it.first + circle1.centerX()) to (it.second + circle1.centerY()) }.toList()
        val p1b = getVector(angle1b, radius1).let { (it.first + circle1.centerX()) to (it.second + circle1.centerY()) }.toList()
        val p2a = getVector(angle2a, radius2).let { (it.first + circle2.centerX()) to (it.second + circle2.centerY()) }.toList()
        val p2b = getVector(angle2b, radius2).let { (it.first + circle2.centerX()) to (it.second + circle2.centerY()) }.toList()

        val totalRadius = (radius1 + radius2)
        val d2Base = min(
                max(topSpreadFactor, bottomSpreadFactor) * handleRate,
                getVectorLength(p1a[0], p1a[1], p2a[0], p2a[1]) / totalRadius)

        // case circles are overlapping:
        val d2 = d2Base * min(1.0f, d * 2 / (radius1 + radius2))

        val r1 = radius1 * d2
        val r2 = radius2 * d2

        val pi2 = fPI / 2
        val sp1 = getVector(angle1a - pi2, r1).toList()
        val sp2 = getVector(angle2a + pi2, r2).toList()
        val sp3 = getVector(angle2b - pi2, r2).toList()
        val sp4 = getVector(angle1b + pi2, r1).toList()

        // move bottom point to bar top border
        val yOffset = (abs(topBorder - p1a[1]) * riseRatio) - 1
        val fp1a = p1a.let { l -> listOf(l[0], l[1] - yOffset) }
        val fp1b = p1b.let { l -> listOf(l[0], l[1] - yOffset) }

        with(path) {
            reset()
            moveTo(fp1a[0], fp1a[1] + cornerRadius)
            lineTo(fp1a[0], fp1a[1])
            cubicTo(fp1a[0] + sp1[0], fp1a[1] + sp1[1], p2a[0] + sp2[0], p2a[1] + sp2[1], p2a[0], p2a[1])
            lineTo(circle2.centerX(), circle2.centerY())
            lineTo(p2b[0], p2b[1])
            cubicTo(p2b[0] + sp3[0], p2b[1] + sp3[1], fp1b[0] + sp4[0], fp1b[1] + sp4[1], fp1b[0], fp1b[1])
            lineTo(fp1b[0], fp1b[1] + cornerRadius)
            close()
        }

        with(canvas) {
            drawPath(path, paint)
            drawOval(circle2, paint)
        }
    }

    private fun drawText(canvas: Canvas, paint: Paint,
                         text: String, align: Paint.Align, color: Int, offset: Float,
                         holderRect: RectF, textRect: Rect) {
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

    private fun showLabel(distance: Float) {
        val top = barVerticalOffset - distance
        val labelVOffset = (topCircleDiameter - labelRectDiameter) / 2f

        val animation = ValueAnimator.ofFloat(rectTopCircle.top, top)
        animation.addUpdateListener {
            val value = it.animatedValue as Float
            rectTopCircle.offsetTo(rectTopCircle.left, value)
            rectLabel.offsetTo(rectLabel.left, value + labelVOffset)
            invalidate()
        }
        animation.duration = duration
        animation.interpolator = OvershootInterpolator()
        animation.start()
    }

    private fun hideLabel() {
        val labelVOffset = (topCircleDiameter - labelRectDiameter) / 2f
        val animation = ValueAnimator.ofFloat(rectTopCircle.top, barVerticalOffset)
        animation.addUpdateListener {
            val value = it.animatedValue as Float
            rectTopCircle.offsetTo(rectTopCircle.left, value)
            rectLabel.offsetTo(rectLabel.left, value + labelVOffset)
            invalidate()
        }
        animation.duration = duration
        animation.start()
    }

}