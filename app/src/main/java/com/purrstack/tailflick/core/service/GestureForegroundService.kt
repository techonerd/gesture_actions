package com.purrstack.tailflick.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.purrstack.tailflick.R
import com.purrstack.tailflick.core.action.ActionRegistry
import com.purrstack.tailflick.core.action.CameraActionHandler
import com.purrstack.tailflick.core.action.TorchActionHandler
import com.purrstack.tailflick.core.gesture.ChopGestureDetector
import com.purrstack.tailflick.core.gesture.GestureEngine
import com.purrstack.tailflick.core.gesture.TwistGestureDetector
import com.purrstack.tailflick.core.settings.SettingsManager

/**
 * Foreground Service Daemon alternative with customizable notification persistence.
 */
class GestureForegroundService : Service() {

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

    companion object {
        const val CHANNEL_ID = "gesture_actions_channel"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, GestureForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, GestureForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        gestureEngine.stopListening()
        settingsManager.unregisterListener(prefsChangeListener)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Tailflick gesture detection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Monitors chop-chop and twist gestures in background"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Tailflick is active")
        .setContentText("Listening for Chop Chop & Double Twist gestures")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(false) // Dismissable per user configuration
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}
