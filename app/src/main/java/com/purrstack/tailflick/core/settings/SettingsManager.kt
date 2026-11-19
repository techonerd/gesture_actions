package com.purrstack.tailflick.core.settings

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("gesture_actions_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_CHOP_ENABLED = "chop_enabled"
        const val KEY_TWIST_ENABLED = "twist_enabled"
        const val KEY_CHOP_SENSITIVITY = "chop_sensitivity"
        const val KEY_TWIST_SENSITIVITY = "twist_sensitivity"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_CAMERA_OPTION = "camera_option"
        const val KEY_THEME_STYLE = "theme_style"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DAEMON_ACTIVE = "daemon_active"
    }

    var isChopEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHOP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_CHOP_ENABLED, value).apply()

    var isTwistEnabled: Boolean
        get() = prefs.getBoolean(KEY_TWIST_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TWIST_ENABLED, value).apply()

    var chopSensitivity: Float
        get() = prefs.getFloat(KEY_CHOP_SENSITIVITY, 28.0f)
        set(value) = prefs.edit().putFloat(KEY_CHOP_SENSITIVITY, value).apply()

    var twistSensitivity: Float
        get() = prefs.getFloat(KEY_TWIST_SENSITIVITY, 8.5f)
        set(value) = prefs.edit().putFloat(KEY_TWIST_SENSITIVITY, value).apply()

    var isVibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, value).apply()

    var cameraOption: String
        get() = prefs.getString(KEY_CAMERA_OPTION, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_CAMERA_OPTION, value).apply()

    var themeStyle: String
        get() = prefs.getString(KEY_THEME_STYLE, "device") ?: "device"
        set(value) = prefs.edit().putString(KEY_THEME_STYLE, value).apply()

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var isDaemonActive: Boolean
        get() = prefs.getBoolean(KEY_DAEMON_ACTIVE, true)
        set(value) = prefs.edit().putBoolean(KEY_DAEMON_ACTIVE, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }
}

