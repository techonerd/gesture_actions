package com.gestureactions.app.core.gesture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Central Sensor Router managing active GestureDetectors.
 */
class GestureEngine(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val activeDetectors = CopyOnWriteArrayList<GestureDetector>()
    private var isListening = false

    fun registerDetector(detector: GestureDetector) {
        if (!activeDetectors.contains(detector)) {
            activeDetectors.add(detector)
            if (isListening) {
                bindSensorListener(detector)
            }
        }
    }

    fun unregisterDetector(detector: GestureDetector) {
        if (activeDetectors.contains(detector)) {
            sensorManager.unregisterListener(detector)
            activeDetectors.remove(detector)
        }
    }

    fun startListening() {
        if (isListening) return
        isListening = true
        activeDetectors.forEach { detector ->
            bindSensorListener(detector)
        }
    }

    fun stopListening() {
        if (!isListening) return
        isListening = false
        activeDetectors.forEach { detector ->
            sensorManager.unregisterListener(detector)
            detector.resetState()
        }
    }

    private fun bindSensorListener(detector: GestureDetector) {
        // Prefer the wake-up variant: non-wake-up sensors stop delivering events once the CPU sleeps (screen off)
        val sensor = sensorManager.getDefaultSensor(detector.targetSensorType, true)
            ?: sensorManager.getDefaultSensor(detector.targetSensorType)
        if (sensor != null) {
            // SENSOR_DELAY_GAME for high accuracy; maxReportLatencyUs for hardware batching
            sensorManager.registerListener(
                detector,
                sensor,
                SensorManager.SENSOR_DELAY_GAME,
                50_000 // 50ms batching latency
            )
        }
    }
}
