package com.gestureactions.app.core.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Auto-restarts Gesture Service on phone reboot.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            GestureForegroundService.startService(context)
        }
    }
}
