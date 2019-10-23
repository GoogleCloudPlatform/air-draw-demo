package com.jamesward.airdraw

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.android.synthetic.main.activity_content.view.*

class DrawingCanvas(context: Context?, attr: AttributeSet) : View(context, attr) {

    var drawing: Boolean = false
//    var previousPoint: PointF = PointF(0f,0f)
    val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val drawingPath = Path()
    private var cachedBitmap: Bitmap? = null

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 70.0f
        paint.color = Color.WHITE
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeCap = Paint.Cap.ROUND
    }

    fun getBitmap():Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        cachedBitmap = bitmap
        val canvas = Canvas(bitmap)
        draw(canvas)
//        invalidate()
        return bitmap
    }

    fun setBitmap(bitmap: Bitmap) {
        cachedBitmap = bitmap
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var handled = false
        if (event != null) {
            when {
                event.action == MotionEvent.ACTION_DOWN -> {
                    drawing = true
    //                previousPoint = PointF(event.x, event.y)
                    drawingPath.moveTo(event.x, event.y)
                    handled = true
                }
                event.action == MotionEvent.ACTION_MOVE -> {
                    //todo: needed?  var newPoint = PointF(event.x, event.y)
                    drawingPath.lineTo(event.x, event.y)
                    invalidate()
                    handled = true
                }
                event.action == MotionEvent.ACTION_UP -> {
                    drawing = false
                    handled = true
                }
            }
        }
        return handled
    }

    fun clear() {
        drawingPath.reset()
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        // todo: draw bitmap
        canvas!!.drawPath(drawingPath, paint)
//        if (cachedBitmap != null) {
//            canvas.scale(.5f, .5f)
//            canvas.drawBitmap(cachedBitmap, 0f, 0f, null)
//        }
    }
}
