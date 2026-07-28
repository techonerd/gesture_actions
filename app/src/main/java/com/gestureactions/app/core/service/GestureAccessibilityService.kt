package com.gestureactions.app.core.service

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.view.accessibility.AccessibilityEvent
import com.gestureactions.app.core.action.ActionRegistry
import com.gestureactions.app.core.action.CameraActionHandler
import com.gestureactions.app.core.action.TorchActionHandler
import com.gestureactions.app.core.gesture.ChopGestureDetector
import com.gestureactions.app.core.gesture.GestureEngine
import com.gestureactions.app.core.gesture.TwistGestureDetector
import com.gestureactions.app.core.settings.SettingsManager

/**
 * Golden Standard Accessibility Daemon Service
 * Guarantees process persistence (OOM Score FOREGROUND_APP) so Android OS never kills gesture listeners.
 */
class GestureAccessibilityService : AccessibilityService() {

    private lateinit var gestureEngine: GestureEngine
    private lateinit var actionRegistry: ActionRegistry
    private lateinit var settingsManager: SettingsManager

    private var chopDetector: ChopGestureDetector? = null
    private var twistDetector: TwistGestureDetector? = null

    private val prefsChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            SettingsManager.KEY_CHOP_ENABLED -> chopDetector?.isEnabled = settingsManager.isChopEnabled
            SettingsManager.KEY_TWIST_ENABLED -> twistDetector?.isEnabled = settingsManager.isTwistEnabled
            SettingsManager.KEY_CHOP_SENSITIVITY -> chopDetector?.setSensitivity(settingsManager.chopSensitivity)
            SettingsManager.KEY_TWIST_SENSITIVITY -> twistDetector?.setSensitivity(settingsManager.twistSensitivity)
            SettingsManager.KEY_DAEMON_ACTIVE -> {
                if (settingsManager.isDaemonActive) {
                    gestureEngine.startListening()
                } else {
                    gestureEngine.stopListening()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        settingsManager = SettingsManager(this)
        gestureEngine = GestureEngine(this)
        actionRegistry = ActionRegistry(this)

        val torchHandler = TorchActionHandler()
        val cameraHandler = CameraActionHandler()

        actionRegistry.registerMapping("chop_chop", torchHandler)
        actionRegistry.registerMapping("double_twist", cameraHandler)

        // Initialize static hardware listeners
        TorchActionHandler.registerTorchCallback(this)
        CameraActionHandler.registerAvailabilityCallback(this)

        val chop = ChopGestureDetector(settingsManager.chopSensitivity) {
            actionRegistry.dispatchAction("chop_chop")
        }.apply {
            isEnabled = settingsManager.isChopEnabled
        }
        chopDetector = chop

        val twist = TwistGestureDetector(settingsManager.twistSensitivity) {
            actionRegistry.dispatchAction("double_twist")
        }.apply {
            isEnabled = settingsManager.isTwistEnabled
        }
        twistDetector = twist

        gestureEngine.registerDetector(chop)
        gestureEngine.registerDetector(twist)

        settingsManager.registerListener(prefsChangeListener)

        if (settingsManager.isDaemonActive) {
            gestureEngine.startListening()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Event pass-through
    }

    override fun onInterrupt() {
        gestureEngine.stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureEngine.stopListening()
        settingsManager.unregisterListener(prefsChangeListener)
    }
}
