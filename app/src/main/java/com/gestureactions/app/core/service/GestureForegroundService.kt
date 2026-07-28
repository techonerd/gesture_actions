package com.gestureactions.app.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gestureactions.app.R
import com.gestureactions.app.core.action.ActionRegistry
import com.gestureactions.app.core.action.CameraActionHandler
import com.gestureactions.app.core.action.TorchActionHandler
import com.gestureactions.app.core.gesture.ChopGestureDetector
import com.gestureactions.app.core.gesture.GestureEngine
import com.gestureactions.app.core.gesture.TwistGestureDetector
import com.gestureactions.app.core.settings.SettingsManager

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "GestureActions Status Daemon",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors chop-chop and twist gestures in background"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("GestureActions Daemon Active")
        .setContentText("Listening for Chop Chop & Double Twist gestures")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setOngoing(false) // Dismissable per user configuration
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()
}
