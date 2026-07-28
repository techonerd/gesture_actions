package com.gestureactions.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val OneUIDarkColorScheme = darkColorScheme(
    primary = Color(0xFF2C6EE8),
    secondary = Color(0xFF5C95FF),
    background = Color(0xFF0C0E14),
    surface = Color(0xFF1B1E2B),
    onPrimary = Color.White,
    onBackground = Color(0xFFEAEFFC),
    onSurface = Color(0xFFEAEFFC)
)

val OneUILightColorScheme = lightColorScheme(
    primary = Color(0xFF1E5BBF),
    secondary = Color(0xFF2C6EE8),
    background = Color(0xFFF4F6FB),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onBackground = Color(0xFF121826),
    onSurface = Color(0xFF121826)
)

val OneUICardShape = RoundedCornerShape(26.dp)
val OneUIPillShape = RoundedCornerShape(30.dp)
