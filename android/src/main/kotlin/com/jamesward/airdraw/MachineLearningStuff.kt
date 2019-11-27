package com.jamesward.airdraw

import android.app.Activity
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
//import android.widget.TextView
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.jamesward.airdraw.data.ImageResult
import com.jamesward.airdraw.data.LabelAnnotation
import com.jamesward.airdraw.data.Orientation
import com.jamesward.airdraw.data.json
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


class MachineLearningStuff(val assets: AssetManager, activity: Activity, val serverUrl: String) {

    private var inputImageWidth: Int = 0 // will be inferred from TF Lite model
    private var inputImageHeight: Int = 0 // will be inferred from TF Lite model
    private var modelInputSize: Int = 0 // will be inferred from TF Lite model
    lateinit private var interpreter: Interpreter
    private var orientationSensorMaybe: OrientationSensor? = null

    data class ResultsItem(val text: String, val confidence: Float)
    companion object {
        val resultsList = ArrayList<ResultsItem>(10)
        var resultsBitmap: Bitmap? = null
    }


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
                       resultsListener: () -> Unit) {

        if (detectShapes) {
            val firebaseImage = FirebaseVisionImage.fromBitmap(bitmap)
            val labeler =
                    if (local) {
                        FirebaseVision.getInstance().onDeviceImageLabeler
                    } else {
                        FirebaseVision.getInstance().cloudImageLabeler
                    }
            labeler.processImage(firebaseImage).addOnSuccessListener { list ->

                resultsList.clear()
                val labelAnnotations = resultsList.map { LabelAnnotation(it.text, it.confidence) }
                for (item in list) {
                    resultsList.add(ResultsItem(item.text, item.confidence))
                }
                resultsListener()
                uploadResults(labelAnnotations, bitmap)
            }
        } else {
            if (local) {
                MainScope().launch {
                    checkDigit(bitmap, resultsListener)
                }
            } else {
                resultsList.clear()
            }
        }
    }

    suspend fun checkDigit(bitmap: Bitmap, resultsListener: () -> Unit) {
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
        resultsList.clear()
        for (result in digitSequence) {
            println("${result.first}: ${result.second * 100}%")
            resultsList.add(ResultsItem(result.first, result.second))
        }
        resultsBitmap = null
        resultsListener()
        val labelAnnotations = digitSequence.map { LabelAnnotation(it.first.toString(), it.second) }
        uploadResults(labelAnnotations, bitmap)
    }

    private fun uploadResults(labelAnnotations: List<LabelAnnotation>, bitmap: Bitmap) {

        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)

        val byteArrayBitmapStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayBitmapStream)

        val imageResult = ImageResult(byteArrayBitmapStream.toByteArray(), labelAnnotations)

        val url = serverUrl + "/show"
        println(url)
        Fuel.post(url)
                .timeoutRead(60 * 1000)
                .jsonBody(imageResult.json())
                .response { result ->
                    println(result)
                }
    }

    private val sensorManager: SensorManager by lazy {
        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    fun sensorAction(on: Boolean, postResult: () -> Unit) {
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

                val url = serverUrl + "/draw"
                println(url)
                Fuel.post(url)
                        .timeoutRead(60 * 1000)
                        .jsonBody(orientationSensor.readings.json())
                        .responseString { result ->
                            result.fold({ json ->
                                val imageResult = ImageResult.fromJson(json)
                                imageResult?.let {
                                    val bitmap = BitmapFactory.decodeByteArray(it.image, 0, it.image.size)
                                    // TODO
                                    // drawingCanvas.setBitmap(bitmap)
                                    resultsBitmap = bitmap

                                    resultsList.clear()
                                    for (annotation in it.labelAnnotations) {
                                        resultsList.add(ResultsItem(annotation.description, annotation.score))
                                    }
                                    postResult()
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

