package com.jamesward.airdraw

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.ToggleButton
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json


class MainActivity: AppCompatActivity() {

    var orientationSensorMaybe: OrientationSensor? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
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
                // todo: externalize config
                Fuel.post("https://air-draw-5yonijgw4a-uc.a.run.app/draw")
                        .jsonBody(json)
                        .response { result ->
                            println(result)
                        }

                orientationSensorMaybe = null
            }
        }
    }

}

