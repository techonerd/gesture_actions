package com.purrstack.tailflick

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.purrstack.tailflick.core.service.GestureForegroundService
import com.purrstack.tailflick.core.settings.SettingsManager
import com.purrstack.tailflick.ui.screens.HomeScreen
import com.purrstack.tailflick.ui.theme.AppTheme
import com.purrstack.tailflick.ui.theme.OEMThemeStyle
import com.purrstack.tailflick.ui.theme.getSystemOemStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The foreground service is the only gesture daemon, so make sure it runs whenever the engine is on
        if (SettingsManager(this).isDaemonActive) {
            GestureForegroundService.startService(this)
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val settings = remember { SettingsManager(context) }

            var themeStyleState by remember { mutableStateOf(settings.themeStyle) }
            var themeModeState by remember { mutableStateOf(settings.themeMode) }

            DisposableEffect(settings) {
                val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        SettingsManager.KEY_THEME_STYLE -> themeStyleState = settings.themeStyle
                        SettingsManager.KEY_THEME_MODE -> themeModeState = settings.themeMode
                    }
                }
                settings.registerListener(listener)
                onDispose {
                    settings.unregisterListener(listener)
                }
            }

            val currentOemStyle = if (themeStyleState == "device") {
                getSystemOemStyle()
            } else {
                OEMThemeStyle.MATERIAL_YOU
            }

            val isDarkTheme = when (themeModeState) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            AppTheme(
                oemStyle = currentOemStyle,
                darkTheme = isDarkTheme
            ) {
                HomeScreen(
                    currentOEMStyle = currentOemStyle,
                    onOEMStyleSelected = { }
                )
            }
        }
    }
}

