package com.jamesward.airdraw

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_content.view.*

class DrawingCanvas(context: Context?, attr: AttributeSet) : View(context, attr) {

    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawingPath = Path()
    private var cachedBitmap: Bitmap? = null

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 70.0f
        paint.color = Color.BLACK
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun getBitmap():Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        println("bitmap 0,0 = " + Integer.toHexString(bitmap.getPixel(0, 0)))
        val canvas = Canvas(bitmap)
        draw(canvas)
        println("bitmap 0,0 = " + Integer.toHexString(bitmap.getPixel(0, 0)))
        return bitmap
    }

    fun setBitmap(bitmap: Bitmap) {
        cachedBitmap = bitmap
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        cachedBitmap = null
        var handled = false
        if (event != null) {
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    drawingPath.moveTo(event.x, event.y)
                    handled = true
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    drawingPath.lineTo(event.x, event.y)
                    invalidate()
                    handled = true
                }
                event.action == MotionEvent.ACTION_UP -> {
                    handled = true
                }
            }
        }
        return handled
    }

    fun clear() {
        drawingPath.reset()
        cachedBitmap = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.drawPath(drawingPath, paint)
//        canvas?.let {
//            if (cachedBitmap != null)
//                it.drawBitmap(cachedBitmap!!, 0f, 0f, null)
//            else
//                it.drawPath(drawingPath, paint)
//        }
    }
}
