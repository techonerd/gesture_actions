package com.gestureactions.app.core.action

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.gestureactions.app.core.settings.SettingsManager

/**
 * Toggles Phone Flashlight / Torch via CameraManager, synchronized with actual hardware state.
 */
class TorchActionHandler : ActionHandler {

    override val id: String = "action_torch"
    override val displayName: String = "🔦 Toggle Flashlight / Torch"

    companion object {
        @Volatile
        private var isTorchOn = false
        @Volatile
        private var isTorchAvailable = true
        private var isCallbackRegistered = false
        private var activeCameraId: String? = null

        fun registerTorchCallback(context: Context) {
            if (isCallbackRegistered) return
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            try {
                activeCameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: cameraManager.cameraIdList.firstOrNull() ?: "0"

                cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        android.util.Log.d("TorchActionHandler", "onTorchModeChanged: id=$cameraId enabled=$enabled")
                        // Only track the flash we toggle; a front flash reporting "off" must not overwrite it
                        if (cameraId != activeCameraId) return
                        isTorchAvailable = true
                        isTorchOn = enabled
                    }

                    override fun onTorchModeUnavailable(cameraId: String) {
                        // Camera is in use by another app (e.g. camera app open); torch cannot be toggled
                        if (cameraId != activeCameraId) return
                        isTorchAvailable = false
                        isTorchOn = false
                    }
                }, null)
                isCallbackRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun isTorchActive(): Boolean = isTorchOn
    }

    override fun execute(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        
        registerTorchCallback(context)
        if (!isTorchAvailable) return

        try {
            val cameraId = activeCameraId ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: cameraManager.cameraIdList.firstOrNull() ?: return

            val targetState = !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)

            val settings = SettingsManager(context)
            if (settings.isVibrationEnabled) {
                triggerHapticFeedback(context)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun triggerHapticFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 50, 30)
                val amplitudes = intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 30, 50, 30), -1)
            }
        }
    }
}

