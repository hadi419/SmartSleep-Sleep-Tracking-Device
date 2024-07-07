package com.example.dreamcatch
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class ClockView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val clockRadius = 200f
    private val centerX: Float
    private val centerY: Float
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        numberPaint.color = Color.BLACK
        numberPaint.textSize = 30f
        numberPaint.textAlign = Paint.Align.CENTER

        linePaint.color = Color.BLACK
        linePaint.strokeWidth = 5f
        linePaint.strokeCap = Paint.Cap.ROUND

        centerX = (width / 2).toFloat()
        centerY = (height / 2).toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.WHITE)

        for (i in 1..12) {
            val number = i.toString()
            val angle = Math.toRadians((i - 3) * 30.0)
            val x = centerX + clockRadius * cos(angle).toFloat()
            val y = centerY + clockRadius * sin(angle).toFloat()

            canvas.drawText(number, x, y + numberPaint.textSize / 3, numberPaint)
        }

        for (i in 0..11) {
            val angle = Math.toRadians(i * 30.0)
            val startX = centerX + (clockRadius - 20) * cos(angle).toFloat()
            val startY = centerY + (clockRadius - 20) * sin(angle).toFloat()
            val stopX = centerX + clockRadius * cos(angle).toFloat()
            val stopY = centerY + clockRadius * sin(angle).toFloat()

            canvas.drawLine(startX, startY, stopX, stopY, linePaint)
        }
    }
}
