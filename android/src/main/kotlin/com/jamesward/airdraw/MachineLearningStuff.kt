package com.jamesward.airdraw

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.jamesward.airdraw.data.LabelAnnotation
import com.jamesward.airdraw.data.Orientation
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class MachineLearningStuff(private val assets: AssetManager, private val activity: Activity) {

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model
    private lateinit var interpreter: Interpreter
    private var orientationSensorMaybe: OrientationSensor? = null

    init {
        initializeInterpreter()
    }

    @Throws(IOException::class)
    private fun loadModelFile(): ByteBuffer {
        val fileDescriptor = assets.openFd("mnist.tflite")
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {
        // Load the TF Lite model
        val model = loadModelFile()

        // Initialize TF Lite Interpreter with NNAPI enabled
        val options = Interpreter.Options()
        options.setUseNNAPI(true)
        val interpreter = Interpreter(model, options)

        // Read input shape from model file
        val inputShape = interpreter.getInputTensor(0).shape()
        inputImageWidth = inputShape[1]
        inputImageHeight = inputShape[2]
        modelInputSize = 4 * inputImageWidth * inputImageHeight

        // Finish interpreter initialization
        this.interpreter = interpreter
    }

    @ExperimentalCoroutinesApi
    fun localDetection(local: Boolean, bitmap: Bitmap, detectShapes: Boolean,
                       resultsListener: (List<LabelAnnotation>) -> Unit) {

        if (detectShapes) {
            val firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
            val labeler = if (local) {
                FirebaseVision.getInstance().onDeviceImageLabeler
            } else {
                FirebaseVision.getInstance().cloudImageLabeler
            }

            labeler.processImage(firebaseImage).addOnSuccessListener { list ->
                val resultsList = list.map { LabelAnnotation(it.text, it.confidence) }

                resultsListener(resultsList)
            }
        } else {
            if (local) {
                MainScope().launch {
                    checkDigit(bitmap, resultsListener)
                }
            }
        }
    }

    private suspend fun checkDigit(bitmap: Bitmap, resultsListener: (List<LabelAnnotation>) -> Unit) {
        lateinit var digitSequence: List<Pair<String,Float>>
        val resizedImage = Bitmap.createScaledBitmap(bitmap, inputImageWidth, inputImageHeight, true)
        val byteBuffer = convertBitmapToByteBuffer(resizedImage)
        val result = Array(1) { FloatArray(10) }
        withContext(Dispatchers.IO) {
            interpreter.run(byteBuffer, result)
        }

        digitSequence = result[0].asSequence().mapIndexed {
            index: Int, value: Float -> Pair(index.toString(), value) }.
                sortedByDescending { it.second }.toList()
        // From Tor: alternatively, could create second Array of indices to hold the sorted digits,
        // Then sort both arrays with custom comparator

        val resultsList = arrayListOf<LabelAnnotation>()

        for (resultPair in digitSequence) {
            println("${resultPair.first}: ${resultPair.second * 100}%")
            resultsList.add(LabelAnnotation(resultPair.first, resultPair.second))
        }

        resultsListener(resultsList)
    }

    private val sensorManager: SensorManager by lazy {
        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun sensorAction(on: Boolean, postResult: (List<Orientation>) -> Unit) {
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

                postResult(orientationSensor.readings)

                orientationSensorMaybe = null
            }
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

            if (!orientationAngles[1].isNaN()) {
                val orientation = Orientation(orientationAngles[0], orientationAngles[1], e.timestamp)
                readings.add(orientation)
            }
        }
    }

}


fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(bitmap.width * bitmap.height * 4)
    byteBuffer.order(ByteOrder.nativeOrder())

    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    for (pixelValue in pixels) {
        val r = (pixelValue shr 16 and 0xFF)
        val g = (pixelValue shr 8 and 0xFF)
        val b = (pixelValue and 0xFF)

        // Convert RGB to grayscale and normalize pixel value to [0..1]
        val normalizedPixelValue = (r + g + b) / 3.0f / 255.0f
        byteBuffer.putFloat(normalizedPixelValue)
    }

    return byteBuffer
}
