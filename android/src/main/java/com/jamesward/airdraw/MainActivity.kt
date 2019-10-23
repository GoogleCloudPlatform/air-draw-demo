/*
 * Copyright 2018 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jamesward.airdraw

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class MainActivity: AppCompatActivity() {

    var orientationSensorMaybe: OrientationSensor? = null
    lateinit var clearButton: Button
    lateinit var localLabel: Button
    lateinit var drawingCanvas: DrawingCanvas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        clearButton = findViewById(R.id.clearButton)
        drawingCanvas = findViewById(R.id.drawingCanvas)
        localLabel = findViewById(R.id.localIdButton)

        initializeInterpreter()
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager): ByteBuffer {
        val fileDescriptor = assetManager.openFd("mnist.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {
        // Load the TF Lite model
        val assetManager = assets
        val model = loadModelFile(assetManager)

        // Initialize TF Lite Interpreter with NNAPI enabled
        val options = Interpreter.Options()
        options.setUseNNAPI(true)
        val interpreter = Interpreter(model, options)

        // Read input shape from model file
        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = 4 * inputImageWidth * inputImageHeight * 1

        // Finish interpreter initialization
        this.interpreter = interpreter
//        isInitialized = true
//        Log.d(TAG, "Initialized TFLite interpreter.")
    }

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model
    lateinit private var interpreter: Interpreter

    fun clearClick(view: View) {
        drawingCanvas?.clear()
        bestGuess.text = ""
        secondGuess.text = ""
        thirdGuess.text = ""
        fourthGuess.text = ""
    }

    fun checkDigit(bitmap: Bitmap): Array<FloatArray> {
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)
        val result = Array(1) { FloatArray(10) }
        interpreter?.run(byteBuffer, result)
//        result.sort()

        return result
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(modelInputSize)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputImageWidth * inputImageHeight)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        for (pixelValue in pixels) {
            val r = (pixelValue shr 16 and 0xFF)
            val g = (pixelValue shr 8 and 0xFF)
            val b = (pixelValue and 0xFF)
//            println("RGB pixel = $r, $g, $b")

            // Convert RGB to grayscale and normalize pixel value to [0..1]
            val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
            byteBuffer.putFloat(normalizedPixelValue)
        }

        return byteBuffer
    }

    fun localLabelClick(view: View) {


        val bitmap = drawingCanvas.getBitmap()
        var digitData = checkDigit(bitmap)
        val digitSequence = digitData[0].asSequence().mapIndexed {
            index: Int, value: Float -> Pair(index, value) }.sortedByDescending { it.second }.toList()
        // From Tor: alternatively, could create second Array of indices to hold the sorted digits,
        // Then sort both arrays with custom comparator
//        var digitDataMap = digitData[0].map {  }
        for (result in digitSequence) {
            println("${result.first}: ${result.second * 100}%")
        }
//        var digitDataSub: FloatArray = digitData[0]
//        println("digitData = $digitData")
//        var maxIndex = digitDataSub.indices.maxBy { digitDataSub[it] } ?: -1
//        println("Best guess: $maxIndex, confidence = ${digitDataSub[maxIndex]}")

        postGuess(bestGuess, digitSequence[0])
        postGuess(secondGuess, digitSequence[1])
        postGuess(thirdGuess, digitSequence[2])
        postGuess(fourthGuess, digitSequence[3])


//        digitData.sort()
//        digitData.forEach { digitResults ->
//            for (i in 0 until digitResults.size) {
//                println("$i: ${digitResults[i] * 10}%")
//            }
//        }

//        var firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
//        var labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler()
//        labeler.processImage(firebaseImage).addOnSuccessListener { list ->
//            for (item in list) {
//                println("local labeled item text, id, confidence = ${item.text}, ${item.entityId}, ${item.confidence}")
//            }
//        }
    }

    private fun postGuess(textView: TextView, guess: Pair<Int, Float>) {
        val value = guess.second * 100
        textView.setText("${guess.first}:" + "   %3.2f%%".format(value))
    }

    fun cloudLabelClick(view: View) {
        var bitmap = drawingCanvas.getBitmap()
        var firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
        var labeler = FirebaseVision.getInstance().getCloudImageLabeler()
        labeler.processImage(firebaseImage).addOnSuccessListener { list ->
            for (item in list) {
                println("cloud labeled item text, id, confidence = ${item.text}, ${item.entityId}, ${item.confidence}")
            }
        }.addOnFailureListener {
            println("FAILURE: $it")
        }.addOnCanceledListener {
            println("canceled!")
        }
    }

    @Serializable
    data class Orientation(val azimuth: Float, val pitch: Float, val timestamp: Long)

    class OrientationSensor: SensorEventListener {
        val readings: MutableList<Orientation> = ArrayList()

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

        override fun onSensorChanged(event: SensorEvent?) {
            event?.let { e ->
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, e.values)
                val orientationAngles = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                // the azimuth goes from -PI to PI potentially causing orientations to "cross over" from -PI to PI
                // to avoid this we convert negative readings to positive resulting in a range 0 to PI*2
                val absAzimuth = if (orientationAngles[0] < 0)
                    orientationAngles[0] + (Math.PI.toFloat() * 2)
                else
                    orientationAngles[0]
                val orientation = Orientation(absAzimuth, orientationAngles[1], e.timestamp)
                readings.add(orientation)
            }
        }
    }

    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    @UnstableDefault
    fun drawClick(view: View) {
        val on = (view as ToggleButton).isChecked

        if (on) {
            orientationSensorMaybe = OrientationSensor()

            sensorManager.registerListener(
                    orientationSensorMaybe,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        else {
            orientationSensorMaybe?.let { orientationSensor ->
                sensorManager.unregisterListener(orientationSensorMaybe)

                val json = Json.stringify(Orientation.serializer().list, orientationSensor.readings.toList())
                val url = resources.getString(R.string.draw_url)
                println(url)
                Fuel.post(url)
                        .jsonBody(json)
                        .response { result ->
                            println(result)
                        }

                orientationSensorMaybe = null
            }
        }
    }

}

