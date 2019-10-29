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

import com.jamesward.airdraw.data.*
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class MainActivity: AppCompatActivity() {

    private var orientationSensorMaybe: OrientationSensor? = null

    lateinit var clearButton: Button
    lateinit var localLabel: Button
    lateinit var drawingCanvas: DrawingCanvas
    lateinit var objectDetectionSpinner: Spinner

    private var detectShapes = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        clearButton = findViewById(R.id.clearButton)
        drawingCanvas = findViewById(R.id.drawingCanvas)
        localLabel = findViewById(R.id.localIdButton)
        objectDetectionSpinner = findViewById(R.id.objectDetectionSpinner)

        setupSpinner()

        initializeInterpreter()
    }

    private fun setupSpinner() {
        ArrayAdapter.createFromResource(this, R.array.objectDetectionChoices,
                android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            objectDetectionSpinner.adapter = adapter
        }
        objectDetectionSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                var itemString = parent?.getItemAtPosition(position)
                println("itemString = $itemString")
                if (itemString == "Shape") {
                    detectShapes = true
                } else {
                    detectShapes = false
                }
            }
        }
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
        drawingCanvas.clear()
        bestGuess.text = ""
        secondGuess.text = ""
        thirdGuess.text = ""
        fourthGuess.text = ""
    }

    fun checkDigit(bitmap: Bitmap): Array<FloatArray> {
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)
        val result = Array(1) { FloatArray(10) }
        interpreter.run(byteBuffer, result)
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
        lateinit var digitSequence: List<Pair<String,Float>>

        if (detectShapes) {
            var firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
            var labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler()
            labeler.processImage(firebaseImage).addOnSuccessListener { list ->
                list.sortByDescending {
                    it.confidence
                }
//                var labelSequence = list.asSequence().mapIndexed {
//                    index: Int, value: Float -> Pair(index.toString(), value) }.sortedByDescending {  }.toList()

                for (item in list) {
                    println("local labeled item text, id, confidence = ${item.text}, ${item.entityId}, ${item.confidence}")
                }

                if (list.size > 0) bestGuess.setText("${list[0].text}:  ${list[0].confidence}")
                if (list.size > 1) secondGuess.setText("${list[1].text}:  ${list[1].confidence}")
                if (list.size > 2) thirdGuess.setText("${list[2].text}:  ${list[2].confidence}")
                if (list.size > 3) fourthGuess.setText("${list[3].text}:  ${list[3].confidence}")
//                digitSequence = List<Pair<String, Float>>()
            }
        } else {
            // digit detection
            val digitData = checkDigit(bitmap)
            digitSequence = digitData[0].asSequence().mapIndexed {
                index: Int, value: Float -> Pair(index.toString(), value) }.sortedByDescending { it.second }.toList()
            // From Tor: alternatively, could create second Array of indices to hold the sorted digits,
            // Then sort both arrays with custom comparator
//        var digitDataMap = digitData[0].map {  }
            for (result in digitSequence) {
                println("${result.first}: ${result.second * 100}%")
            }
            postGuess(bestGuess, digitSequence[0])
            postGuess(secondGuess, digitSequence[1])
            postGuess(thirdGuess, digitSequence[2])
            postGuess(fourthGuess, digitSequence[3])
            val labelAnnotations = digitSequence.map { LabelAnnotation(it.first.toString(), it.second) }

            val buffer = ByteBuffer.allocate(bitmap.byteCount)
            bitmap.copyPixelsToBuffer(buffer)

            val byteArrayBitmapStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayBitmapStream)

            val imageResult = ImageResult(byteArrayBitmapStream.toByteArray(), labelAnnotations)

            val url = resources.getString(R.string.draw_url) + "/show"
            println(url)
            Fuel.post(url)
                    .timeoutRead(60 * 1000)
                    .jsonBody(imageResult.json())
                    .response { result ->
                        println(result)
                    }
        }
    }

    private fun displayDetectionResults(list: List<FirebaseVisionImageLabel>) {
        if (list.size > 0) postGuess(bestGuess, list[0])
        if (list.size > 1) postGuess(secondGuess, list[1])
        if (list.size > 2) postGuess(thirdGuess, list[2])
        if (list.size > 3) postGuess(fourthGuess, list[3])
    }

    private fun postGuess(textView: TextView, label: FirebaseVisionImageLabel) {
        val value = label.confidence * 100
        textView.text = "${label.text}:" + "   %3.2f%%".format(value)
    }


    private fun postGuess(textView: TextView, guess: Pair<String, Float>) {
        val value = guess.second * 100
        textView.text = "${guess.first}:" + "   %3.2f%%".format(value)
    }

    fun cloudLabelClick(view: View) {
        val bitmap = drawingCanvas.getBitmap()
        val firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
        val labeler = FirebaseVision.getInstance().cloudImageLabeler
        labeler.processImage(firebaseImage).addOnSuccessListener { list ->
            displayDetectionResults(list)
            for (item in list) {
                println("cloud labeled item text, id, confidence = ${item.text}, ${item.entityId}, ${item.confidence}")
            }
        }
    }

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

                val pitch = if (orientationAngles[1].isNaN())
                    0f
                else
                    orientationAngles[1]

                val orientation = Orientation(absAzimuth, pitch, e.timestamp)
                readings.add(orientation)
            }
        }
    }

    private val sensorManager: SensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

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

                val url = resources.getString(R.string.draw_url) + "/draw"
                println(url)
                Fuel.post(url)
                        .timeoutRead(60 * 1000)
                        .jsonBody(orientationSensor.readings.json())
                        .responseString { result ->
                            result.fold({ json ->
                                val imageResult = ImageResult.fromJson(json)
                                imageResult?.let {
                                    val bitmap = BitmapFactory.decodeByteArray(it.image, 0, it.image.size)
                                    drawingCanvas.setBitmap(bitmap)

                                    fun setView(textView: TextView, labelAnnotation: LabelAnnotation?) {
                                        if (labelAnnotation != null) {
                                            postGuess(textView, Pair(labelAnnotation.description, labelAnnotation.score))
                                        }
                                        else {
                                            textView.text = ""
                                        }
                                    }

                                    setView(bestGuess, it.labelAnnotations.getOrNull(0))
                                    setView(secondGuess, it.labelAnnotations.getOrNull(1))
                                    setView(thirdGuess, it.labelAnnotations.getOrNull(2))
                                    setView(fourthGuess, it.labelAnnotations.getOrNull(3))
                                }
                            }, {
                                println(it)
                            })
                        }

                orientationSensorMaybe = null
            }
        }
    }

}

