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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Text
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.onActive
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.unit.dp
import com.jamesward.airdraw.data.Orientation
import io.micronaut.context.annotation.Value
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Post
import io.micronaut.http.client.annotation.Client
import kotlinx.coroutines.*
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject
    var drawService: DrawService? = null

    @Value("\${drawurl}")
    var drawUrl: String? = null

    private var orientationSensorMaybe: OrientationSensor? = null

    @Client("\${drawurl}")
    interface DrawService {
        @Post("/draw")
        suspend fun draw(@Body readings: List<Orientation>): HttpResponse<Unit>
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

    suspend fun drawClick(on: Boolean) {
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

                val status = drawService?.draw(orientationSensor.readings)?.status()
                println("Server Response: $status")

                orientationSensorMaybe = null
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("drawurl = $drawUrl")

        setContent {
            Surface(color = MaterialTheme.colors.background) {
                Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    ToggleButton(::drawClick)
                }
            }
        }
    }
}

@Composable
fun ToggleButton(onChange: suspend (Boolean) -> Unit) {
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    onActive { onDispose { scope.cancel() } }

    val on = remember { mutableStateOf(false) }

    @Composable
    fun color() = if (on.value) MaterialTheme.colors.secondary else MaterialTheme.colors.primary

    @Composable
    fun text() = if (on.value) "Stop Drawing" else "Start Drawing"

    Button(onClick = {
        on.value = !on.value

        scope.launch {
            onChange(on.value)
        }
    }, modifier = Modifier.fillMaxWidth(0.75f).height(128.dp), backgroundColor = color()) {
        Text(text())
    }
}
