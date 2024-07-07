package com.example.dreamcatch


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class DonutSelectorView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var centerX = 0f
    private var centerY = 0f
    private var donutRadius = 0f
    private var arcRadius = 0f
    private var arcStartAngle = -90f
    private var arcSweepAngle = 180f
    private var hourChangeListener: OnHourChangeListener? = null

    private val donutPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false
    private var isResizing = false
    private var touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        donutPaint.color = Color.parseColor("#00283A63")
        arcPaint.color = Color.parseColor("#CCA9AFBD")
        arcPaint.style = Paint.Style.STROKE
        arcPaint.strokeWidth = 30f
        arcPaint.strokeCap = Paint.Cap.ROUND

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        donutRadius = w / 4f
        arcRadius = donutRadius + donutRadius - 1/4f
    }

    fun setOnHourChangeListener(listener: OnHourChangeListener?) {
        hourChangeListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawCircle(centerX, centerY, donutRadius, donutPaint)

        val path = Path()
        val arcStartX = centerX + arcRadius * cos(Math.toRadians(arcStartAngle.toDouble())).toFloat()
        val arcStartY = centerY + arcRadius * sin(Math.toRadians(arcStartAngle.toDouble())).toFloat()
        path.moveTo(arcStartX, arcStartY)
        val arcEndAngle = arcStartAngle + arcSweepAngle
        val arcEndX = centerX + arcRadius * cos(Math.toRadians(arcEndAngle.toDouble())).toFloat()
        val arcEndY = centerY + arcRadius * sin(Math.toRadians(arcEndAngle.toDouble())).toFloat()
        path.arcTo(centerX - arcRadius, centerY - arcRadius, centerX + arcRadius, centerY + arcRadius, arcStartAngle, arcSweepAngle, true)
        canvas.drawPath(path, arcPaint)
        notifyHourChanged()
    }

    private fun notifyHourChanged() {
        hourChangeListener?.onHourChanged(getStartHour(), getEndHour())
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isDragging = isInsideArc(x, y)
                isResizing = isNearArcEnds(x, y)
                lastTouchX = x
                lastTouchY = y
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val newAngle = calculateAngle(x, y)
                    val deltaAngle = newAngle - calculateAngle(lastTouchX, lastTouchY)
                    arcStartAngle += deltaAngle
                    invalidate()
                } else if (isResizing) {
                    val distance = calculateDistance(x, y)
                    arcSweepAngle = distance.coerceIn(0f, donutRadius * 2f)
                    invalidate()
                }
                lastTouchX = x
                lastTouchY = y
                notifyHourChanged()
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                isResizing = false
                notifyHourChanged()
            }
        }
        return true
    }

    private fun isInsideArc(x: Float, y: Float): Boolean {
        val angle = calculateAngle(x, y)
        return angle in arcStartAngle..(arcStartAngle + arcSweepAngle)
    }

    private fun isNearArcEnds(x: Float, y: Float): Boolean {
        val startEndPoints = calculateArcEndPoints(arcStartAngle, arcRadius)
        val endEndPoints = calculateArcEndPoints(arcStartAngle + arcSweepAngle, arcRadius)

        val startDistance = sqrt((x - startEndPoints.first).pow(2) + (y - startEndPoints.second).pow(2))
        val endDistance = sqrt((x - endEndPoints.first).pow(2) + (y - endEndPoints.second).pow(2))

        return startDistance < touchSlop || endDistance < touchSlop
    }

    private fun calculateAngle(x: Float, y: Float): Float {
        return Math.toDegrees(atan2(y - centerY, x - centerX).toDouble()).toFloat()
    }



    private fun calculateDistance(x: Float, y: Float): Float {
        return sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
    }

    private fun calculateArcEndPoints(angle: Float, radius: Float): Pair<Float, Float> {
        val x = centerX + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val y = centerY + radius * sin(Math.toRadians(angle.toDouble())).toFloat()
        return Pair(x, y)
    }


    fun getStartHour(): Int {
        val startAngle = calculateAngleFromEndpoints(centerX, centerY, arcRadius, arcStartAngle)

        var startHour = ((startAngle / 360) * 12).toInt()
        startHour = startHour + 3
        if(startHour>12){
            startHour -= 12
        }
        return if (startHour == 0) 12 else startHour
    }

    fun getEndHour(): Int {
        val endAngle = calculateAngleFromEndpoints(centerX, centerY, arcRadius, arcStartAngle + arcSweepAngle)
        var endHour = ((endAngle / 360) * 12).toInt()
        endHour = endHour + 3
        if (endHour>12){
            endHour -= 12
        }
        return if (endHour == 0) 12 else endHour
    }

    private fun calculateAngleFromEndpoints(centerX: Float, centerY: Float, radius: Float, angle: Float): Float {
        val x = centerX + radius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val y = centerY + radius * sin(Math.toRadians(angle.toDouble())).toFloat()

        val deltaX = x - centerX
        val deltaY = y - centerY
        val angleRadians = atan2(deltaY.toDouble(), deltaX.toDouble())
        val angleDegrees = Math.toDegrees(angleRadians).toFloat()
        return if (angleDegrees < 0) angleDegrees + 360 else angleDegrees
    }



}
