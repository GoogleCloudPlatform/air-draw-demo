package com.jamesward.airdraw

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingCanvas(context: Context?, attr: AttributeSet) : View(context, attr) {

    var drawing: Boolean = false
//    var previousPoint: PointF = PointF(0f,0f)
    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val drawingPath = Path()
    var cachedBitmap: Bitmap? = null

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 70.0f
        paint.color = Color.WHITE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun getBitmap():Bitmap {
        var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        cachedBitmap = bitmap
        var canvas = Canvas(bitmap)
        draw(canvas)
//        invalidate()
        return bitmap
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var handled = false
        if (event != null) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                drawing = true
//                previousPoint = PointF(event.x, event.y)
                drawingPath.moveTo(event.x, event.y)
                handled = true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                var newPoint = PointF(event.x, event.y)
                drawingPath.lineTo(event.x, event.y)
                invalidate()
                handled = true
            } else if (event.action == MotionEvent.ACTION_UP) {
                drawing = false
                handled = true
            }
        }
        return handled
    }

    fun clear() {
        drawingPath.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas!!.drawPath(drawingPath, paint)
//        if (cachedBitmap != null) {
//            canvas.scale(.5f, .5f)
//            canvas.drawBitmap(cachedBitmap, 0f, 0f, null)
//        }
    }
}