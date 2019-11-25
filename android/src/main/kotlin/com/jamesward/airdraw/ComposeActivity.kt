package com.jamesward.airdraw

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.ui.core.*
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.graphics.*
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.ButtonStyle
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
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
    strokeWidth = 60f
    strokeJoin = StrokeJoin.round
    strokeCap = StrokeCap.round
}
private val path = Path()

@Composable
fun BuildUI() {
    MaterialTheme() {
        Column(crossAxisAlignment = CrossAxisAlignment.Stretch) {
            Row(mainAxisAlignment = MainAxisAlignment.Center,
                    mainAxisSize = LayoutSize.Expand,
                    crossAxisSize = LayoutSize.Expand) {
                val radioOptions = listOf("Shape", "Digit")
                val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
                RadioGroup(
                        options = radioOptions,
                        selectedOption = selectedOption,
                        onSelectedChange = onOptionSelected
                )
                Column (mainAxisSize = LayoutSize.Expand) {
                    Button(text = "Local")
                    if (selectedOption == "Shape") {
                        Button(text = "Cloud")
                    }
                }
            }
            Button(text = "Sensorify")
            DrawingCanvas(path)
            Center {
                Button(text = "Clear", onClick = {
                    path.reset()
                })
            }
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
        Container(modifier = ExpandedHeight, width = 200.dp, height = 200.dp) {
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
