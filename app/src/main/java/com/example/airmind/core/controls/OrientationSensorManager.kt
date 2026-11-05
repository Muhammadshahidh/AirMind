package com.example.airmind.core.controls

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.atan2
import kotlin.math.sqrt

data class OrientationAngles(
    val pitch: Float = 0f, // tilt forward/backward (-90 to +90)
    val roll: Float = 0f,  // tilt left/right (-180 to +180)
    val yaw: Float = 0f    // rotation around vertical axis (0 to 360)
)

class OrientationSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationVectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometerSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _orientationAngles = MutableStateFlow(OrientationAngles())
    val orientationAngles: StateFlow<OrientationAngles> = _orientationAngles.asStateFlow()

    private var isListening = false
    private val rotationMatrix = FloatArray(9)
    private val orientationOutput = FloatArray(3)

    fun startListening() {
        if (isListening || sensorManager == null) return
        val registered = rotationVectorSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        } ?: false

        if (!registered && accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        isListening = true
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationOutput)

            // Convert radians to degrees
            val yaw = Math.toDegrees(orientationOutput[0].toDouble()).toFloat()
            val pitch = Math.toDegrees(orientationOutput[1].toDouble()).toFloat()
            val roll = Math.toDegrees(orientationOutput[2].toDouble()).toFloat()

            // Smooth low-pass interpolation
            val current = _orientationAngles.value
            _orientationAngles.value = OrientationAngles(
                pitch = current.pitch * 0.8f + pitch * 0.2f,
                roll = current.roll * 0.8f + roll * 0.2f,
                yaw = current.yaw * 0.8f + yaw * 0.2f
            )
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            val roll = Math.toDegrees(atan2(ax.toDouble(), sqrt((ay * ay + az * az).toDouble()))).toFloat()
            val pitch = Math.toDegrees(atan2(-ay.toDouble(), az.toDouble())).toFloat()

            val current = _orientationAngles.value
            _orientationAngles.value = current.copy(
                pitch = current.pitch * 0.8f + pitch * 0.2f,
                roll = current.roll * 0.8f + roll * 0.2f
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
