package com.gestureactions.app.core.gesture

import android.hardware.Sensor
import android.hardware.SensorEvent
import kotlin.math.sqrt

/**
 * Moto-Style Chop-Chop Detector (Flashlight Trigger)
 * Uses TYPE_LINEAR_ACCELERATION to ignore gravity and implements a robust peak hysteresis detector.
 */
class ChopGestureDetector(
    private var thresholdJerk: Float = 28.0f,
    private val onGestureTriggered: () -> Unit
) : GestureDetector {

    override val id: String = "chop_chop"
    override val displayName: String = "Chop Chop (Flashlight)"
    override val targetSensorType: Int = Sensor.TYPE_LINEAR_ACCELERATION
    override var isEnabled: Boolean = true

    private var peakCount = 0
    private var lastPeakTime = 0L

    override fun setSensitivity(value: Float) {
        this.thresholdJerk = value
    }

    override fun resetState() {
        peakCount = 0
        lastPeakTime = 0L
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null || event.sensor.type != Sensor.TYPE_LINEAR_ACCELERATION) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val mag = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()

        if (mag > thresholdJerk) {
            // Cooldown of 120ms to prevent double-counting samples from the same swing/peak
            if (now - lastPeakTime > 120) {
                if (peakCount == 0) {
                    peakCount = 1
                    lastPeakTime = now
                } else if (peakCount == 1) {
                    val dt = now - lastPeakTime
                    if (dt in 150..850) { // Wider window for natural human variations
                        resetState()
                        onGestureTriggered() // Double Chop Verified!
                    } else {
                        // Treat this as the new first peak
                        peakCount = 1
                        lastPeakTime = now
                    }
                }
            }
        }

        // Timeout reset: if the second peak is not registered in time
        if (peakCount > 0 && now - lastPeakTime > 850) {
            resetState()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}


