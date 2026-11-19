package com.purrstack.tailflick.core.settings

import android.content.Context
import android.provider.Settings

class SamsungGesturesManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("samsung_gestures_fallback", Context.MODE_PRIVATE)

    companion object {
        const val WAKE_GESTURE = "wake_gesture_enabled"
        const val DOUBLE_TAP_ON = "double_tap_to_wake"
        const val DOUBLE_TAP_OFF = "double_tap_to_sleep"
        const val SMART_ALERT = "smart_alert"
        const val EASY_MUTE = "easy_mute"
        const val PALM_SWIPE = "palm_swipe_to_capture"
        const val SMART_STAY = "smart_stay"
    }

    fun hasWriteSettingsPermission(): Boolean = Settings.System.canWrite(context)

    fun getSetting(key: String, default: Boolean = true): Boolean {
        return try {
            val value = Settings.System.getInt(context.contentResolver, key, -1)
            if (value == -1) {
                prefs.getBoolean(key, default)
            } else {
                value == 1
            }
        } catch (e: Exception) {
            prefs.getBoolean(key, default)
        }
    }

    fun setSetting(key: String, enabled: Boolean): Boolean {
        prefs.edit().putBoolean(key, enabled).apply()
        if (!hasWriteSettingsPermission()) return false
        return try {
            Settings.System.putInt(context.contentResolver, key, if (enabled) 1 else 0)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

