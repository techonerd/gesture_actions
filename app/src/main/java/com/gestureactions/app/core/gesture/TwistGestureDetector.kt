package com.gestureactions.app.core.gesture

import android.hardware.Sensor
import android.hardware.SensorEvent
import kotlin.math.sign

/**
 * Moto-Style Double Wrist Twist Detector (Camera Launcher)
 * Analyzes angular roll velocity along Y-axis and registers alternating peaks.
 */
class TwistGestureDetector(
    private var thresholdGyro: Float = 8.5f,
    private val onGestureTriggered: () -> Unit
) : GestureDetector {

    override val id: String = "double_twist"
    override val displayName: String = "Quick Twist (Camera)"
    override val targetSensorType: Int = Sensor.TYPE_GYROSCOPE
    override var isEnabled: Boolean = true

    private var peakCount = 0
    private var lastPeakTime = 0L
    private var lastPeakSign = 0.0f
    private var waitingForCoolDown = false
    private val coolDownThreshold = 1.0f // must drop below 1.0 rad/s for next peak

    override fun setSensitivity(value: Float) {
        this.thresholdGyro = value
    }

    override fun resetState() {
        peakCount = 0
        lastPeakTime = 0L
        lastPeakSign = 0.0f
        waitingForCoolDown = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!isEnabled || event == null || event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val wy = event.values[1] // Y-axis (longitudinal axis) angular velocity
        val now = System.currentTimeMillis()

        if (waitingForCoolDown) {
            if (Math.abs(wy) < coolDownThreshold) {
                waitingForCoolDown = false
            }
        } else {
            if (Math.abs(wy) > thresholdGyro) {
                val currentSign = sign(wy)
                if (peakCount == 0) {
                    peakCount = 1
                    lastPeakTime = now
                    lastPeakSign = currentSign
                    waitingForCoolDown = true
                } else if (peakCount == 1) {
                    val dt = now - lastPeakTime
                    if (dt in 150..750 && currentSign != lastPeakSign) {
                        resetState()
                        onGestureTriggered() // Double Twist Verified!
                    } else if (dt > 750) {
                        // Restart sequence with this peak
                        peakCount = 1
                        lastPeakTime = now
                        lastPeakSign = currentSign
                        waitingForCoolDown = true
                    } else {
                        // Noise/same peak direction, trigger cooldown
                        waitingForCoolDown = true
                    }
                }
            }
        }

        if (peakCount > 0 && now - lastPeakTime > 850) {
            resetState()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

