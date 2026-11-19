package com.purrstack.tailflick.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val HyperOSDarkColorScheme = darkColorScheme(
    primary = Color(0xFF0070F3),
    secondary = Color(0xFF7928CA),
    background = Color(0xFF080B12),
    surface = Color(0xFF161C2C),
    onPrimary = Color.White,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

val HyperOSLightColorScheme = lightColorScheme(
    primary = Color(0xFF0066FF),
    secondary = Color(0xFF00D2FF),
    background = Color(0xFFEEF2F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

val HyperOSCardShape = RoundedCornerShape(18.dp)
val HyperOSPillShape = RoundedCornerShape(20.dp)
