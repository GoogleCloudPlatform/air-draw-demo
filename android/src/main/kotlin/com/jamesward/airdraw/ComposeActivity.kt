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
import androidx.ui.material.surface.Surface
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.tooling.preview.Preview
import com.jamesward.airdraw.data.LabelAnnotation
import kotlinx.coroutines.ExperimentalCoroutinesApi

lateinit var machineLearningStuff: MachineLearningStuff

class ComposeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BuildUI()
        }
        machineLearningStuff = MachineLearningStuff(assets, this)
    }
}

private val fingerPaint = Paint().apply {
    color = Color.White
    style = PaintingStyle.stroke
    strokeWidth = 60f
    strokeJoin = StrokeJoin.round
    strokeCap = StrokeCap.round
}
private val path = Path()
private var bitmap: Bitmap? = null
private var sensorifying = false
private val guesses = ArrayList<String>(4)

@Model
class Guesses(var guessList: List<LabelAnnotation> = ArrayList())

@UseExperimental(ExperimentalCoroutinesApi::class)
@Composable
fun BuildUI(guesses: Guesses = Guesses()) {
    println("BuildUI")
    MaterialTheme() {
        val invalidator = +invalidate
        Column(Spacing(8.dp), crossAxisAlignment = CrossAxisAlignment.Stretch) {
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
                                println("results = " + it.toString())
                                guesses.guessList = it
                                invalidator()
                            }
                        })
                        if (selectedOption == "Shape") {
                            HeightSpacer(8.dp)
                            Button(text = "Cloud", onClick = {
                                machineLearningStuff.localDetection(false, bitmap!!,
                                        selectedOption == "Shape") {
                                    println("results = " + it.toString())
                                    guesses.guessList = it
                                    invalidator()
                                }
                            })
                        }
                    }
                }
            }
            println("sensorifying = $sensorifying")
            Button(text = "Sensorify", onClick = {
                sensorifying = !sensorifying
                val on = sensorifying
                invalidate
                machineLearningStuff.sensorAction(on) {
                    println("results = " + it.toString())
                }
            }, style = ButtonStyle(if (sensorifying) Color.Gray else Color.Green,
                    shape = RectangleShape))
            HeightSpacer(8.dp)
            GuessDisplay(guesses)
            HeightSpacer(8.dp)
            DrawingCanvas(path)
            HeightSpacer(8.dp)
            Button(text = "Clear", onClick = {
                path.reset()
                invalidator()
            })
            HeightSpacer(8.dp)
        }
    }
}

@Composable
fun GuessDisplay(state: Guesses) {
    for (i in 0..3) {
        lateinit var label: String
        if (i < state.guessList.size) {
            val item = state.guessList[i]
            label = "${item.description}: " + "   %3.2f%%".format(item.score * 100)
        } else {
            label = if (i == 0) "No Results" else ""
        }
        Text(label, style = if (i == 0)
            TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold) else null)
    }
}


// TODO: Handle case where there was no drag, to place a point at the clicked point
// This entails PointerInputHandler/etc to handle non-gesture case
class MyDragObserver(val dragPath: Path, val recompose: () -> Unit): DragObserver {
    override fun onStart(downPosition: PxPosition) {
        dragPath.moveTo(downPosition.x.value, downPosition.y.value)
    }

    override fun onDrag(dragDistance: PxPosition): PxPosition {
        dragPath.relativeLineTo(dragDistance.x.value, dragDistance.y.value)
        recompose()
        return dragDistance
    }
}


@Composable
fun DrawingCanvas(path: Path) {
    val invalidate = +invalidate

    RawDragGestureDetector(dragObserver = MyDragObserver(path, invalidate)) {
        Surface(color = Color.Black) {
            Container(modifier = ExpandedHeight, width = 200.dp, height = 350.dp) {
                Draw { canvas, parentSize ->
                    println("redrawing")
                    canvas.drawPath(path, fingerPaint)
                    if (bitmap == null || bitmap?.width != parentSize.width.value.toInt()) {
                        bitmap = Bitmap.createBitmap(parentSize.width.value.toInt(),
                                parentSize.height.value.toInt(), Bitmap.Config.ARGB_8888)
                    }
                    val bitmapCanvas = android.graphics.Canvas(bitmap!!)
                    bitmapCanvas.drawPath(path.toFrameworkPath(), fingerPaint.asFrameworkPaint())
                }
            }
        }
    }
}
