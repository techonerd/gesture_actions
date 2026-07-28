package com.gestureactions.app.core.action

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import com.gestureactions.app.core.settings.SettingsManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Launches Camera activity with keyguard bypass flags, supporting camera toggles and preferences.
 */
class CameraActionHandler : ActionHandler {

    override val id: String = "action_camera"
    override val displayName: String = "📷 Launch Camera"

    companion object {
        private val unavailableCameras = ConcurrentHashMap.newKeySet<String>()
        private var isCallbackRegistered = false

        fun registerAvailabilityCallback(context: Context) {
            if (isCallbackRegistered) return
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
            try {
                cameraManager.registerAvailabilityCallback(object : CameraManager.AvailabilityCallback() {
                    override fun onCameraAvailable(cameraId: String) {
                        unavailableCameras.remove(cameraId)
                    }

                    override fun onCameraUnavailable(cameraId: String) {
                        unavailableCameras.add(cameraId)
                    }
                }, null)
                isCallbackRegistered = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun execute(context: Context) {
        registerAvailabilityCallback(context)

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val settings = SettingsManager(context)

        var isFrontActive = false
        var isRearActive = false

        for (id in unavailableCameras) {
            try {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    isFrontActive = true
                } else if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    isRearActive = true
                }
            } catch (e: Exception) {
                // Ignore
            }
        }

        // Determine target facing: toggle if camera is active, otherwise use preference
        val targetIsFront = when {
            isFrontActive -> false
            isRearActive -> true
            else -> {
                when (settings.cameraOption) {
                    "front" -> true
                    "rear" -> false
                    else -> { // "auto"
                        val orientation = context.resources.configuration.orientation
                        orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
                    }
                }
            }
        }

        if (settings.isVibrationEnabled) {
            triggerHapticFeedback(context)
        }

        val isLocked = (context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true
        val baseAction = if (isLocked) {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE
        } else {
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
        }

        val intent = Intent(baseAction).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            val facingValue = if (targetIsFront) 1 else 0
            putExtra("android.intent.extras.CAMERA_FACING", facingValue)
            putExtra("android.intent.extra.USE_FRONT_CAMERA", targetIsFront)
            putExtra("android.intent.extras.LENS_FACING_FRONT", facingValue)
            putExtra("front_camera", targetIsFront)
            putExtra("camerafacing", if (targetIsFront) "front" else "rear")
            putExtra("previous_mode", if (targetIsFront) "front" else "rear")
        }

        try {
            val samsungIntent = Intent(intent).apply { setPackage("com.sec.android.app.camera") }
            context.startActivity(samsungIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun triggerHapticFeedback(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        }
    }
}

