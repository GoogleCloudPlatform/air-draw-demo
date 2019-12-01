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

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.jamesward.airdraw.data.ImageResult
import com.jamesward.airdraw.data.LabelAnnotation
import com.jamesward.airdraw.data.Orientation
import io.micronaut.context.annotation.Prototype
import io.micronaut.context.annotation.Value
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import io.reactivex.Single
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import javax.inject.Inject

@Client("\${drawurl}")
interface DrawService {
    @Post("/show")
    fun show(@Body imageResult: ImageResult): Single<Unit>

    @Post("/draw")
    fun draw(@Body readings: List<Orientation>): Single<ImageResult>
}

@Prototype
class MainActivity: AppCompatActivity() {

    @Inject
    var drawService: DrawService? = null

    @Value("\${drawurl}")
    var drawUrl: String? = null

    lateinit var clearButton: Button
    lateinit var localLabel: Button
    lateinit var cloudIdButton: Button
    lateinit var drawingCanvas: DrawingCanvas
    lateinit var objectDetectionSpinner: Spinner
    lateinit var machineLearningStuff: MachineLearningStuff

    private var detectShapes = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        clearButton = findViewById(R.id.clearButton)
        drawingCanvas = findViewById(R.id.drawingCanvas)
        localLabel = findViewById(R.id.localIdButton)
        cloudIdButton = findViewById(R.id.cloudIdButton)
        objectDetectionSpinner = findViewById(R.id.objectDetectionSpinner)

        setupSpinner()

        machineLearningStuff = MachineLearningStuff(assets, this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        getMenuInflater().inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.composeItem) {
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
            return true
        }
        return false
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

            override fun onItemSelected(parent: AdapterView<*>?, view: View?,
                                        position: Int, id: Long) {
                val itemString = parent?.getItemAtPosition(position)
                detectShapes = itemString == "Shape"
                cloudIdButton.isEnabled = detectShapes
            }
        }
    }

    fun clearClick(view: View) {
        drawingCanvas.clear()
        clearGuesses()
    }

    @ExperimentalCoroutinesApi
    fun localLabelClick(view: View) {
        clearGuesses()

        val bitmap = drawingCanvas.getBitmap()

        machineLearningStuff.localDetection(true, bitmap, detectShapes, displayResult(bitmap))
    }

    private fun clearGuesses() {
        bestGuess.text = ""
        secondGuess.text = ""
        thirdGuess.text = ""
        fourthGuess.text = ""
    }

    @ExperimentalCoroutinesApi
    fun cloudLabelClick(view: View) {
        clearGuesses()

        val bitmap = drawingCanvas.getBitmap()

        machineLearningStuff.localDetection(false, bitmap, detectShapes, displayResult(bitmap))
    }

    fun drawClick(view: View) {
        val on = (view as ToggleButton).isChecked

        machineLearningStuff.sensorAction(on) { orientations ->
            val imageResultMaybe = drawService?.draw(orientations)?.blockingGet()
            imageResultMaybe?.let { imageResult ->
                displayResults(imageResult)
            }
        }
    }

    private fun displayResult(bitmap: Bitmap): (List<LabelAnnotation>) -> Unit {

        val buffer = ByteBuffer.allocate(bitmap.byteCount)
        bitmap.copyPixelsToBuffer(buffer)

        val byteArrayBitmapStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, byteArrayBitmapStream)

        return { labelAnnotations: List<LabelAnnotation> ->
            val imageResult = ImageResult(byteArrayBitmapStream.toByteArray(), labelAnnotations)

            displayResults(imageResult)

            drawService?.show(imageResult)?.subscribe()?.dispose()
        }
    }

    private fun displayResults(imageResult: ImageResult) {

        val bitmap = BitmapFactory.decodeByteArray(imageResult.image, 0, imageResult.image.size)

        drawingCanvas.setBitmap(bitmap)

        if (imageResult.labelAnnotations.isEmpty()) {
            bestGuess.text = "No Results"
            return
        }

        fun setGuess(i: Int, guess: TextView) {
            imageResult.labelAnnotations.getOrNull(i).let { result ->
                if (result != null) {
                    guess.text = "${result.description}: ${result.score}"
                } else {
                    guess.text = ""
                }
            }
        }

        setGuess(0, bestGuess)
        setGuess(1, secondGuess)
        setGuess(2, thirdGuess)
        setGuess(3, fourthGuess)
    }

}
