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

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import kotlinx.android.synthetic.main.activity_content.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*


class MainActivity: AppCompatActivity() {

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

        machineLearningStuff = MachineLearningStuff(assets, this,
                resources.getString(R.string.draw_url))
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
                var itemString = parent?.getItemAtPosition(position)
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

        machineLearningStuff.localDetection(true, bitmap, detectShapes) {
            displayResults()
        }
    }

    private fun clearGuesses() {
        bestGuess.text = ""
        secondGuess.text = ""
        thirdGuess.text = ""
        fourthGuess.text = ""
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

    fun cloudLabelClick(view: View) {
        clearGuesses()

        val bitmap = drawingCanvas.getBitmap()

        machineLearningStuff.localDetection(false, bitmap, detectShapes) {
            displayResults()
        }
    }

    fun drawClick(view: View) {
        val on = (view as ToggleButton).isChecked

        machineLearningStuff.sensorAction(on) {
            displayResults()
        }

    }

    private fun displayResults() {
        if (MachineLearningStuff.resultsList.size == 0) {
            bestGuess.setText("No Results")
        }
        drawingCanvas.setBitmap(MachineLearningStuff.resultsBitmap)

        for ((index, result) in MachineLearningStuff.resultsList.withIndex()) {
            when (index) {
                0 -> bestGuess.setText("${result.text}: ${result.confidence}")
                1 -> secondGuess.setText("${result.text}: ${result.confidence}")
                2 -> thirdGuess.setText("${result.text}: ${result.confidence}")
                3 -> fourthGuess.setText("${result.text}: ${result.confidence}")
            }
        }
    }

}

