package com.purrstack.tailflick.core.gesture

import android.hardware.SensorEventListener

/**
 * Base interface for all modular gesture detection algorithms.
 * New custom gestures (e.g. Double-tap back, Flip face-down, Proximity wave)
 * implement this interface to plug into the GestureEngine router cleanly.
 */
interface GestureDetector : SensorEventListener {
    val id: String
    val displayName: String
    val targetSensorType: Int
    var isEnabled: Boolean
    
    fun setSensitivity(value: Float)
    fun resetState()
}
