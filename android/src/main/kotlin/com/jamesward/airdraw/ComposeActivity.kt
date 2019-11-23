package com.jamesward.airdraw

import androidx.ui.graphics.Path
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.ui.core.*
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.layout.Column
import androidx.ui.layout.Container
import androidx.ui.layout.Row
import androidx.ui.material.Button
import androidx.ui.material.MaterialTheme
import androidx.ui.tooling.preview.Preview

class ComposeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuildUI()
        }
    }
}

private val fingerPaint = Paint().apply {
    color = Color.Black
    style = PaintingStyle.stroke
    strokeWidth = 5f
}
private val path = Path()

@Composable
fun BuildUI() {
    MaterialTheme() {
        Column() {
            Row() {
                Button(text = "Thing 1")
                Button(text = "Thing 2")
                Button(text = "Thing 3")
            }
            Button(text = "Thing thing 4")
            DrawingCanvas(path)
            Button(text = "Thing thing 5")
        }
    }
}

class MyDragObserver(val dragPath: Path, val recompose: () -> Unit): DragObserver {
    override fun onStart(downPosition: PxPosition) {
        dragPath.moveTo(downPosition.x.value, downPosition.y.value)
    }

    override fun onDrag(dragDistance: PxPosition): PxPosition {
        dragPath.relativeLineTo(dragDistance.x.value, dragDistance.y.value)
        recompose()
        return dragDistance
    }

    override fun onStop(velocity: PxPosition) {
        // TODO: Handle case where there was no drag, to place a point at the clicked point
    }
}


@Composable
fun DrawingCanvas(path: Path) {
    val invalidate = +invalidate
    RawDragGestureDetector(dragObserver = MyDragObserver(path, invalidate)) {
        Container(width = 200.dp, height = 200.dp) {
            Draw { canvas, parentSize ->
                canvas.drawPath(path, fingerPaint)
            }
        }
    }
}

@Preview
@Composable
fun PreviewDrawingCanvas() {
    val path = Path()
    DrawingCanvas(path)
}
