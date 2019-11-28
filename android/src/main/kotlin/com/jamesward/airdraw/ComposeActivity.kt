package com.jamesward.airdraw

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.*
import androidx.ui.core.*
import androidx.ui.core.gesture.DragObserver
import androidx.ui.core.gesture.RawDragGestureDetector
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.*
import androidx.ui.layout.*
import androidx.ui.material.Button
import androidx.ui.material.ButtonStyle
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
import androidx.ui.tooling.preview.Preview

lateinit var machineLearningStuff: MachineLearningStuff

class ComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuildUI()
        }
        machineLearningStuff = MachineLearningStuff(assets, this,
                resources.getString(R.string.draw_url))
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
private var bitmap: Bitmap? = null
private var sensorifying = false

@Composable
fun BuildUI() {
    MaterialTheme() {
        Column(Spacing(16.dp), crossAxisAlignment = CrossAxisAlignment.Stretch) {
            FlexRow(mainAxisAlignment = MainAxisAlignment.Center) {
                val radioOptions = listOf("Shape", "Digit")
                val (selectedOption, onOptionSelected) = +state { radioOptions[0] }
                inflexible {
                    RadioGroup(
                            options = radioOptions,
                            selectedOption = selectedOption,
                            onSelectedChange = onOptionSelected
                    )
                }
                flexible(1f) {
                    Column(crossAxisAlignment = CrossAxisAlignment.Stretch) {
                        Button(text = "Local", onClick = {
                            machineLearningStuff.localDetection(true, bitmap!!,
                                    selectedOption == "Shape") {
                                println("results = " + MachineLearningStuff.resultsList)
                            }
                        })
                        if (selectedOption == "Shape") {
                            HeightSpacer(16.dp)
                            Button(text = "Cloud")
                        }
                    }
                }
            }
            HeightSpacer(16.dp)
            println("sensorifying = $sensorifying")
            Button(text = "Sensorify", onClick = {
                sensorifying = !sensorifying
                val on = sensorifying
                invalidate
                machineLearningStuff.sensorAction(on) {
                    println("results = " + MachineLearningStuff.resultsList)
                }
            }, style = ButtonStyle(if (sensorifying) Color.Gray else Color.Green,
                    shape = RectangleShape))
            DrawingCanvas(path)
            Center {
                Button(text = "Clear", onClick = {
                    path.reset()
                    bitmap?.eraseColor(android.graphics.Color.WHITE)
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
                if (bitmap == null || bitmap?.width != parentSize.width.value.toInt()) {
                    bitmap = Bitmap.createBitmap(parentSize.width.value.toInt(),
                            parentSize.height.value.toInt(), Bitmap.Config.ARGB_8888)
                }
                var bitmapCanvas = android.graphics.Canvas(bitmap!!)
                bitmapCanvas.drawPath(path.toFrameworkPath(), fingerPaint.asFrameworkPaint())
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
