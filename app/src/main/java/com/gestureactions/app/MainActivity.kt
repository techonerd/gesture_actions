package com.gestureactions.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.gestureactions.app.core.settings.SettingsManager
import com.gestureactions.app.ui.screens.HomeScreen
import com.gestureactions.app.ui.theme.AppTheme
import com.gestureactions.app.ui.theme.OEMThemeStyle
import com.gestureactions.app.ui.theme.getSystemOemStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

