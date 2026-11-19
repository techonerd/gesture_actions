package com.purrstack.tailflick.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

enum class OEMThemeStyle {
    ONE_UI,       // Samsung One UI (Rounded cards, squircle shapes)
    HYPER_OS,     // Xiaomi HyperOS (Glassmorphism, gradients)
    MOTO,         // Motorola Style (Deep blacks, teal accents, clean layouts)
    MATERIAL_YOU  // Google Material 3 / Dynamic Monet Palette
}

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    secondary = Color(0xFF60A5FA),
    tertiary = Color(0xFF93C5FD),
    background = Color(0xFF0F1015),
    surface = Color(0xFF1F2230),
    onPrimary = Color.White,
    onBackground = Color(0xFFF0F2F8),
    onSurface = Color(0xFFF0F2F8)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    secondary = Color(0xFF3B82F6),
    tertiary = Color(0xFF1D4ED8),
    background = Color(0xFFF2F4F8),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B)
)

val MotoDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00A2E8),
    secondary = Color(0xFF00D2FF),
    background = Color(0xFF000000),
    surface = Color(0xFF12141C),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val MotoLightColorScheme = lightColorScheme(
    primary = Color(0xFF0070F3),
    secondary = Color(0xFF00A2E8),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

val MotoCardShape = RoundedCornerShape(12.dp)
val MotoPillShape = RoundedCornerShape(24.dp)

@Composable
fun AppTheme(
    oemStyle: OEMThemeStyle = OEMThemeStyle.ONE_UI,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        oemStyle == OEMThemeStyle.MATERIAL_YOU && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        oemStyle == OEMThemeStyle.HYPER_OS -> {
            if (darkTheme) HyperOSDarkColorScheme else HyperOSLightColorScheme
        }
        oemStyle == OEMThemeStyle.ONE_UI -> {
            if (darkTheme) OneUIDarkColorScheme else OneUILightColorScheme
        }
        oemStyle == OEMThemeStyle.MOTO -> {
            if (darkTheme) MotoDarkColorScheme else MotoLightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

fun getSystemOemStyle(): OEMThemeStyle {
    val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
    val brand = Build.BRAND?.lowercase() ?: ""
    return when {
        manufacturer.contains("samsung") || brand.contains("samsung") -> OEMThemeStyle.ONE_UI
        manufacturer.contains("xiaomi") || brand.contains("xiaomi") || manufacturer.contains("redmi") || brand.contains("redmi") || manufacturer.contains("poco") || brand.contains("poco") -> OEMThemeStyle.HYPER_OS
        manufacturer.contains("motorola") || brand.contains("motorola") || manufacturer.contains("lenovo") || brand.contains("lenovo") -> OEMThemeStyle.MOTO
        else -> OEMThemeStyle.MATERIAL_YOU
    }
}

