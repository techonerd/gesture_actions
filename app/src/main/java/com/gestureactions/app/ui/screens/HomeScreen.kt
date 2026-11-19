package com.gestureactions.app.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gestureactions.app.core.action.CameraActionHandler
import com.gestureactions.app.core.action.TorchActionHandler
import com.gestureactions.app.core.gesture.ChopGestureDetector
import com.gestureactions.app.core.gesture.GestureEngine
import com.gestureactions.app.core.gesture.TwistGestureDetector
import com.gestureactions.app.core.settings.SettingsManager
import com.gestureactions.app.core.settings.SamsungGesturesManager
import com.gestureactions.app.ui.theme.OEMThemeStyle
import com.gestureactions.app.ui.theme.getSystemOemStyle
import kotlinx.coroutines.launch

sealed interface Screen {
    data object Main : Screen
    data object FastTorchDetail : Screen
    data object FastTorchSettings : Screen
    data object FastTorchTryIt : Screen
    data object QuickCaptureDetail : Screen
    data object QuickCaptureSettings : Screen
    data object QuickCaptureTryIt : Screen
    data object QuickLaunchDetail : Screen
    data object SidebarDetail : Screen
    data object ThreeFingerDetail : Screen
    data object LiftUnlockDetail : Screen
    data object FlipDndDetail : Screen
    data object PickUpSilenceDetail : Screen
    data object SwipeSplitDetail : Screen
    data object Settings : Screen
    data object Support : Screen
    data object About : Screen
}

data class DynamicGestureItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isSamsungSystem: Boolean = false,
    val samsungKey: String? = null,
    val targetScreen: Screen? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    currentOEMStyle: OEMThemeStyle,
    onOEMStyleSelected: (OEMThemeStyle) -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }
    val scope = rememberCoroutineScope()

    // Preferences State
    var isChopEnabled by remember { mutableStateOf(settings.isChopEnabled) }
    var isTwistEnabled by remember { mutableStateOf(settings.isTwistEnabled) }
    var chopSensitivity by remember { mutableStateOf(settings.chopSensitivity) }
    var twistSensitivity by remember { mutableStateOf(settings.twistSensitivity) }
    var isVibrationEnabled by remember { mutableStateOf(settings.isVibrationEnabled) }
    var cameraOption by remember { mutableStateOf(settings.cameraOption) }
    var themeStyle by remember { mutableStateOf(settings.themeStyle) }
    var themeMode by remember { mutableStateOf(settings.themeMode) }
    var isDaemonActive by remember { mutableStateOf(settings.isDaemonActive) }

    // Navigation Stack
    val backStack = remember { mutableStateListOf<Screen>(Screen.Main) }
    val currentScreen = backStack.lastOrNull() ?: Screen.Main

    // Drawer State
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Device OEM style resolver
    val activeStyle = if (themeStyle == "device") {
        getSystemOemStyle()
    } else {
        OEMThemeStyle.MATERIAL_YOU
    }

    LaunchedEffect(activeStyle) {
        onOEMStyleSelected(activeStyle)
    }

    // Back Handlers
    var lastBackPressTime by remember { mutableStateOf(0L) }

    BackHandler(enabled = currentScreen != Screen.Main) {
        if (backStack.size > 1) {
            backStack.removeLast()
        }
    }

    BackHandler(enabled = currentScreen == Screen.Main) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            (context as? Activity)?.finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BrandLogo(style = activeStyle, size = 48.dp)
                        Column {
                            Text(
                                text = "Moto Gestures",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Premium Tuning App",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = currentScreen == Screen.Main,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.clear()
                            backStack.add(Screen.Main)
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("Settings") },
                        selected = currentScreen == Screen.Settings,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.add(Screen.Settings)
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE91E63)) },
                        label = { Text("Support") },
                        selected = currentScreen == Screen.Support,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.add(Screen.Support)
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("About App") },
                        selected = currentScreen == Screen.About,
                        onClick = {
                            scope.launch { drawerState.close() }
                            backStack.add(Screen.About)
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Made with 🐾 by technoerd",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "© 2026 technoerd. All rights reserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    ) {
        val navigateBack = {
            if (backStack.size > 1) {
                backStack.removeLast()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "screen_navigation"
            ) { screen ->
                when (screen) {
                    Screen.Main -> MainScreenContent(
                        activeStyle = activeStyle,
                        isChopEnabled = isChopEnabled,
                        onChopToggled = {
                            isChopEnabled = it
                            settings.isChopEnabled = it
                        },
                        isTwistEnabled = isTwistEnabled,
                        onTwistToggled = {
                            isTwistEnabled = it
                            settings.isTwistEnabled = it
                        },
                        isDaemonActive = isDaemonActive,
                        onDaemonToggled = {
                            isDaemonActive = it
                            settings.isDaemonActive = it
                            if (it) {
                                com.gestureactions.app.core.service.GestureForegroundService.startService(context)
                            } else {
                                com.gestureactions.app.core.service.GestureForegroundService.stopService(context)
                            }
                        },
                        onDrawerOpen = { scope.launch { drawerState.open() } },
                        onNavigate = { backStack.add(it) }
                    )

                    Screen.FastTorchDetail -> FastTorchDetailScreen(
                        isChopEnabled = isChopEnabled,
                        onChopToggled = {
                            isChopEnabled = it
                            settings.isChopEnabled = it
                        },
                        onNavigateBack = navigateBack,
                        onNavigateSettings = { backStack.add(Screen.FastTorchSettings) },
                        onNavigateTryIt = { backStack.add(Screen.FastTorchTryIt) }
                    )

                    Screen.FastTorchSettings -> FastTorchSettingsScreen(
                        isVibrationEnabled = isVibrationEnabled,
                        onVibrationToggled = {
                            isVibrationEnabled = it
                            settings.isVibrationEnabled = it
                        },
                        chopSensitivity = chopSensitivity,
                        onSensitivityChanged = {
                            chopSensitivity = it
                            settings.chopSensitivity = it
                        },
                        onNavigateBack = navigateBack
                    )

                    Screen.FastTorchTryIt -> TryItScreen(
                        title = "Try it out",
                        instructions = "Hold your phone tightly and make two quick karate chop motions.",
                        hint = "Try it now.",
                        successMsg = "Success! Flashlight Toggled!",
                        gestureId = "chop_chop",
                        settings = settings,
                        onNavigateBack = navigateBack
                    )

                    Screen.QuickCaptureDetail -> QuickCaptureDetailScreen(
                        isTwistEnabled = isTwistEnabled,
                        onTwistToggled = {
                            isTwistEnabled = it
                            settings.isTwistEnabled = it
                        },
                        onNavigateBack = navigateBack,
                        onNavigateSettings = { backStack.add(Screen.QuickCaptureSettings) },
                        onNavigateTryIt = { backStack.add(Screen.QuickCaptureTryIt) }
                    )

                    Screen.QuickCaptureSettings -> QuickCaptureSettingsScreen(
                        cameraOption = cameraOption,
                        onCameraOptionSelected = {
                            cameraOption = it
                            settings.cameraOption = it
                        },
                        twistSensitivity = twistSensitivity,
                        onSensitivityChanged = {
                            twistSensitivity = it
                            settings.twistSensitivity = it
                        },
                        onNavigateBack = navigateBack
                    )

                    Screen.QuickCaptureTryIt -> TryItScreen(
                        title = "Twist twice",
                        instructions = "Hold your phone and twist your wrist twice like you're turning a doorknob.",
                        hint = "Try it now.",
                        successMsg = "Success! Camera Launched!",
                        gestureId = "double_twist",
                        settings = settings,
                        onNavigateBack = navigateBack
                    )

                    Screen.QuickLaunchDetail -> GenericGestureDetailScreen(
                        title = "Quick launch",
                        description = "Tap the back of your phone twice to open your favorite shortcut app or action.",
                        illustration = { animMod -> QuickLaunchAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.SidebarDetail -> GenericGestureDetailScreen(
                        title = "Sidebar",
                        description = "Swipe inward from the edge to access your favorite floating apps and tools.",
                        illustration = { animMod -> SidebarAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.ThreeFingerDetail -> GenericGestureDetailScreen(
                        title = "Three-finger screenshot",
                        description = "Touch and hold the screen with three fingertips to capture an instant screenshot.",
                        illustration = { animMod -> ThreeFingerAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.LiftUnlockDetail -> GenericGestureDetailScreen(
                        title = "Lift to unlock",
                        description = "Lift your phone up from a table and look at the screen to instantly unlock using face detection.",
                        illustration = { animMod -> LiftUnlockAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.FlipDndDetail -> GenericGestureDetailScreen(
                        title = "Flip for Do Not Disturb",
                        description = "Place your phone face down on any flat surface to immediately silence notifications and calls.",
                        illustration = { animMod -> FlipDndAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.PickUpSilenceDetail -> GenericGestureDetailScreen(
                        title = "Pick up to silence",
                        description = "Pick up your phone when it rings to automatically silence the ringer sound.",
                        illustration = { animMod -> PickUpSilenceAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.SwipeSplitDetail -> GenericGestureDetailScreen(
                        title = "Swipe to split",
                        description = "Swipe back and forth across the middle of the display to split your screen into two active app windows.",
                        illustration = { animMod -> SwipeSplitAnimation(modifier = animMod) },
                        onNavigateBack = navigateBack
                    )

                    Screen.Settings -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeModeSelected = {
                            themeMode = it
                            settings.themeMode = it
                        },
                        themeStyle = themeStyle,
                        onThemeStyleSelected = {
                            themeStyle = it
                            settings.themeStyle = it
                        },
                        chopSensitivity = chopSensitivity,
                        onChopSensitivityChanged = {
                            chopSensitivity = it
                            settings.chopSensitivity = it
                        },
                        twistSensitivity = twistSensitivity,
                        onTwistSensitivityChanged = {
                            twistSensitivity = it
                            settings.twistSensitivity = it
                        },
                        onNavigateBack = navigateBack
                    )

                    Screen.Support -> SupportScreen(
                        activeStyle = activeStyle,
                        onNavigateBack = navigateBack
                    )

                    Screen.About -> AboutScreen(
                        activeStyle = activeStyle,
                        onNavigateBack = navigateBack
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Brand Logo
// -----------------------------------------------------------------------------
@Composable
fun BrandLogo(style: OEMThemeStyle, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        when (style) {
            OEMThemeStyle.MOTO -> {
                drawCircle(color = Color(0xFF00A2E8), radius = w / 2)
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.7f)
                    cubicTo(w * 0.22f, h * 0.45f, w * 0.35f, h * 0.3f, w * 0.5f, h * 0.52f)
                    cubicTo(w * 0.65f, h * 0.3f, w * 0.78f, h * 0.45f, w * 0.75f, h * 0.7f)
                    lineTo(w * 0.65f, h * 0.7f)
                    cubicTo(w * 0.63f, h * 0.5f, w * 0.58f, h * 0.45f, w * 0.5f, h * 0.6f)
                    cubicTo(w * 0.42f, h * 0.45f, w * 0.37f, h * 0.5f, w * 0.35f, h * 0.7f)
                    close()
                }
                drawPath(path = path, color = Color.White)
            }

            OEMThemeStyle.ONE_UI -> {
                drawRoundRect(
                    color = Color(0xFF2C6EE8),
                    cornerRadius = CornerRadius(w * 0.3f, h * 0.3f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = w * 0.3f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.15f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }

            OEMThemeStyle.HYPER_OS -> {
                drawRoundRect(
                    color = Color(0xFFFF6700),
                    cornerRadius = CornerRadius(w * 0.25f, h * 0.25f)
                )
                val strokeW = w * 0.08f
                val path = Path().apply {
                    moveTo(w * 0.25f, h * 0.7f)
                    lineTo(w * 0.25f, h * 0.4f)
                    cubicTo(w * 0.28f, h * 0.33f, w * 0.38f, h * 0.33f, w * 0.4f, h * 0.4f)
                    lineTo(w * 0.4f, h * 0.7f)
                    moveTo(w * 0.4f, h * 0.4f)
                    cubicTo(w * 0.43f, h * 0.33f, w * 0.53f, h * 0.33f, w * 0.55f, h * 0.4f)
                    lineTo(w * 0.55f, h * 0.7f)
                    moveTo(w * 0.7f, h * 0.7f)
                    lineTo(w * 0.7f, h * 0.4f)
                }
                drawPath(path = path, color = Color.White, style = Stroke(width = strokeW, cap = StrokeCap.Round))
                drawCircle(color = Color.White, radius = strokeW / 2, center = Offset(w * 0.7f, h * 0.3f))
            }

            OEMThemeStyle.MATERIAL_YOU -> {
                drawCircle(color = Color(0xFF3DDB85), radius = w / 2)
                drawCircle(color = Color.White, radius = w * 0.07f, center = Offset(w * 0.35f, h * 0.45f))
                drawCircle(color = Color.White, radius = w * 0.07f, center = Offset(w * 0.65f, h * 0.45f))
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.35f, h * 0.3f),
                    end = Offset(w * 0.2f, h * 0.15f),
                    strokeWidth = w * 0.06f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(w * 0.65f, h * 0.3f),
                    end = Offset(w * 0.8f, h * 0.15f),
                    strokeWidth = w * 0.06f,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Main Screen Content
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    activeStyle: OEMThemeStyle,
    isChopEnabled: Boolean,
    onChopToggled: (Boolean) -> Unit,
    isTwistEnabled: Boolean,
    onTwistToggled: (Boolean) -> Unit,
    isDaemonActive: Boolean,
    onDaemonToggled: (Boolean) -> Unit,
    onDrawerOpen: () -> Unit,
    onNavigate: (Screen) -> Unit
) {
    val context = LocalContext.current
    val pm = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(pm.isIgnoringBatteryOptimizations(context.packageName))
    }
    var canDrawOverlays by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Dynamic S23 setting values cached locally
    val samsungManager = remember { SamsungGesturesManager(context) }
    var wakeGesture by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.WAKE_GESTURE, false)) }
    var doubleTapOn by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.DOUBLE_TAP_ON, false)) }
    var doubleTapOff by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.DOUBLE_TAP_OFF, false)) }
    var smartAlert by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.SMART_ALERT, false)) }
    var easyMute by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.EASY_MUTE, false)) }
    var palmSwipe by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.PALM_SWIPE, false)) }
    var smartStay by remember { mutableStateOf(samsungManager.getSetting(SamsungGesturesManager.SMART_STAY, false)) }

    // Toggle helper for device settings
    val toggleSamsungSetting = { key: String, currentVal: Boolean, updateState: (Boolean) -> Unit ->
        val success = samsungManager.setSetting(key, !currentVal)
        updateState(!currentVal)
        if (!samsungManager.hasWriteSettingsPermission()) {
            Toast.makeText(context, "Write Settings permission required. Requesting access...", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    // Background sync loop: keeps state in sync with system Settings.System database
    LaunchedEffect(Unit) {
        while (true) {
            isIgnoringBatteryOptimizations = pm.isIgnoringBatteryOptimizations(context.packageName)
            canDrawOverlays = Settings.canDrawOverlays(context)
            if (activeStyle == OEMThemeStyle.ONE_UI) {
                wakeGesture = samsungManager.getSetting(SamsungGesturesManager.WAKE_GESTURE, false)
                doubleTapOn = samsungManager.getSetting(SamsungGesturesManager.DOUBLE_TAP_ON, false)
                doubleTapOff = samsungManager.getSetting(SamsungGesturesManager.DOUBLE_TAP_OFF, false)
                smartAlert = samsungManager.getSetting(SamsungGesturesManager.SMART_ALERT, false)
                easyMute = samsungManager.getSetting(SamsungGesturesManager.EASY_MUTE, false)
                palmSwipe = samsungManager.getSetting(SamsungGesturesManager.PALM_SWIPE, false)
                smartStay = samsungManager.getSetting(SamsungGesturesManager.SMART_STAY, false)
            }
            kotlinx.coroutines.delay(1500)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // Build dynamic device gestures list depending on detected OEM brand
    val dynamicDeviceGestures = remember(activeStyle, wakeGesture, doubleTapOn, doubleTapOff, smartAlert, easyMute, palmSwipe, smartStay) {
        when (activeStyle) {
            OEMThemeStyle.ONE_UI -> listOf(
                DynamicGestureItem("s_wake", "Lift to wake", "Turn on the screen when you pick up your phone.", Icons.Default.PhoneAndroid, true, SamsungGesturesManager.WAKE_GESTURE),
                DynamicGestureItem("s_tap_on", "Double tap to turn on screen", "Turn on the screen when you double tap it.", Icons.Default.TouchApp, true, SamsungGesturesManager.DOUBLE_TAP_ON),
                DynamicGestureItem("s_tap_off", "Double tap to turn off screen", "Double tap an empty space on the Home or Lock screen.", Icons.Default.TapAndPlay, true, SamsungGesturesManager.DOUBLE_TAP_OFF),
                DynamicGestureItem("s_alert", "Alert when phone picked up", "Phone vibrates when picked up after missing a call or message.", Icons.Default.NotificationsActive, true, SamsungGesturesManager.SMART_ALERT),
                DynamicGestureItem("s_mute", "Mute with gestures", "Mute calls/alarms by placing hand over screen or turning face down.", Icons.Default.VolumeMute, true, SamsungGesturesManager.EASY_MUTE),
                DynamicGestureItem("s_palm", "Palm swipe to capture", "Swipe the edge of your hand across screen to take a screenshot.", Icons.Default.Screenshot, true, SamsungGesturesManager.PALM_SWIPE),
                DynamicGestureItem("s_stay", "Keep screen on while viewing", "Keep screen on while your face is viewing the screen.", Icons.Default.Visibility, true, SamsungGesturesManager.SMART_STAY)
            )

            OEMThemeStyle.MOTO -> listOf(
                DynamicGestureItem("m_sidebar", "Sidebar", "Swipe indicator to open shortcut apps sidebar", Icons.Default.ViewSidebar),
                DynamicGestureItem("m_launch", "Quick launch", "Double tap back of phone to open custom app shortcut", Icons.Default.Launch),
                DynamicGestureItem("m_screenshot", "Three-finger screenshot", "Press and hold screen with three fingers to take screenshot", Icons.Default.Crop),
                DynamicGestureItem("m_split", "Swipe to split", "Swipe back and forth across screen to enter split-screen view", Icons.Default.Splitscreen),
                DynamicGestureItem("m_lift", "Lift to unlock", "Lift phone and look at screen to unlock using face detection", Icons.Default.LockOpen),
                DynamicGestureItem("m_silence", "Pick up to silence", "Ringer silences when phone is lifted during incoming calls", Icons.Default.VolumeMute),
                DynamicGestureItem("m_dnd", "Flip for Do Not Disturb", "Place phone face down on flat surface to activate DND mode", Icons.Default.DoNotDisturbOn)
            )

            OEMThemeStyle.HYPER_OS -> listOf(
                DynamicGestureItem("h_double_tap", "Double tap to wake/turn off", "Wake screen or turn off screen with quick double taps", Icons.Default.TouchApp),
                DynamicGestureItem("h_lift", "Lift to wake screen", "Pick up phone to illuminate lockscreen automatically", Icons.Default.PhoneAndroid),
                DynamicGestureItem("h_screenshot", "Three-finger swipe screenshot", "Swipe down with three fingers to capture screenshot", Icons.Default.Screenshot),
                DynamicGestureItem("h_fingerprint", "Fingerprint double tap", "Double tap fingerprint sensor to toggle helper tasks", Icons.Default.Visibility),
                DynamicGestureItem("h_mute", "Flip to silence calls", "Flip phone face down to silence call ringtones", Icons.Default.VolumeMute)
            )

            else -> listOf(
                DynamicGestureItem("g_lift", "Lift to wake screen", "Wakes lockscreen on phone pick up", Icons.Default.PhoneAndroid),
                DynamicGestureItem("g_double_tap", "Double tap to wake", "Turn screen on by double tapping surface", Icons.Default.TouchApp),
                DynamicGestureItem("g_dnd", "Flip face down for DND", "Flipping phone turns on Silent Do Not Disturb", Icons.Default.DoNotDisturbOn),
                DynamicGestureItem("g_shake", "Shake to clear notifications", "Shake phone side-to-side to clear notification tray", Icons.Default.CleaningServices),
                DynamicGestureItem("g_palm", "Palm swipe screenshot", "Swipe palm to trigger capture", Icons.Default.Screenshot)
            )
        }
    }

    val filteredDynamicGestures = remember(searchQuery, dynamicDeviceGestures) {
        if (searchQuery.trim().isEmpty()) {
            dynamicDeviceGestures
        } else {
            dynamicDeviceGestures.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BrandLogo(style = activeStyle, size = 30.dp)
                        Text(
                            text = "Gestures",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDrawerOpen) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(Icons.Default.Search, contentDescription = "Search settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AnimatedVisibility(visible = isSearchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search gestures...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }

            Text(
                text = "Control your phone with simple gestures",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Overlay permission card: lets Double Twist open the camera while the app is in the background
            if (isTwistEnabled && !canDrawOverlays) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = getCardShape(activeStyle),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Allow Display Over Other Apps",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Android only lets apps open the camera from the background with this permission. Needed for Double Twist to work when the app is closed.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open overlay settings", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Battery Optimization Warning Card
            if (!isIgnoringBatteryOptimizations) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = getCardShape(activeStyle),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "Background Protection Alert",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Battery optimizations will terminate the gesture services in the background. Grant Unrestricted access to guarantee 100% gesture reliability.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                    context.startActivity(fallback)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Grant Access", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Service Daemon Controller Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = getCardShape(activeStyle),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isDaemonActive) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Gesture Engine Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isDaemonActive) "Background Daemon Active" else "Engine Stopped (Gestures Disabled)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDaemonActive) Color(0xFF10B981) else Color.Gray
                            )
                        }
                    }
                    Switch(checked = isDaemonActive, onCheckedChange = onDaemonToggled)
                }
            }

            // -----------------------------------------------------------------
            // SECTION 1: Recommended Gestures (Flash and Camera)
            // -----------------------------------------------------------------
            if (searchQuery.trim().isEmpty() ||
                "quick capture".contains(searchQuery, ignoreCase = true) ||
                "camera".contains(searchQuery, ignoreCase = true) ||
                "fast torch".contains(searchQuery, ignoreCase = true) ||
                "flashlight".contains(searchQuery, ignoreCase = true)
            ) {
                Text(
                    text = "Recommended Gestures",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (searchQuery.isEmpty() || "quick capture".contains(searchQuery, ignoreCase = true) || "camera".contains(searchQuery, ignoreCase = true)) {
                        GestureRowItem(
                            title = "Quick capture",
                            subtitle = if (!isDaemonActive) "Disabled (Engine Off)" else if (isTwistEnabled) "On" else "Off",
                            icon = Icons.Default.CameraAlt,
                            isEnabled = if (isDaemonActive) isTwistEnabled else false,
                            switchEnabled = isDaemonActive,
                            alpha = if (isDaemonActive) 1.0f else 0.5f,
                            onToggled = { onTwistToggled(it) },
                            onClick = {
                                if (isDaemonActive) {
                                    onNavigate(Screen.QuickCaptureDetail)
                                } else {
                                    Toast.makeText(context, "Enable Gesture Engine Status to manage gestures.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }

                    if (searchQuery.isEmpty() || "fast torch".contains(searchQuery, ignoreCase = true) || "flashlight".contains(searchQuery, ignoreCase = true)) {
                        GestureRowItem(
                            title = "Fast torch",
                            subtitle = if (!isDaemonActive) "Disabled (Engine Off)" else if (isChopEnabled) "On" else "Off",
                            icon = Icons.Default.FlashlightOn,
                            isEnabled = if (isDaemonActive) isChopEnabled else false,
                            switchEnabled = isDaemonActive,
                            alpha = if (isDaemonActive) 1.0f else 0.5f,
                            onToggled = { onChopToggled(it) },
                            onClick = {
                                if (isDaemonActive) {
                                    onNavigate(Screen.FastTorchDetail)
                                } else {
                                    Toast.makeText(context, "Enable Gesture Engine Status to manage gestures.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // -----------------------------------------------------------------
            // SECTION 2: Dynamic System & Adaptive Gestures
            // -----------------------------------------------------------------
            val sectionHeader = when (activeStyle) {
                OEMThemeStyle.ONE_UI -> "Samsung Motions & Gestures"
                OEMThemeStyle.MOTO -> "Motorola Moto Gestures"
                OEMThemeStyle.HYPER_OS -> "Xiaomi HyperOS Gestures"
                else -> "All Gestures"
            }

            if (filteredDynamicGestures.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sectionHeader,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredDynamicGestures.forEach { gesture ->
                        val isDeviceSetting = gesture.isSamsungSystem && gesture.samsungKey != null

                        val switchState = if (isDeviceSetting) {
                            when (gesture.samsungKey) {
                                SamsungGesturesManager.WAKE_GESTURE -> wakeGesture
                                SamsungGesturesManager.DOUBLE_TAP_ON -> doubleTapOn
                                SamsungGesturesManager.DOUBLE_TAP_OFF -> doubleTapOff
                                SamsungGesturesManager.SMART_ALERT -> smartAlert
                                SamsungGesturesManager.EASY_MUTE -> easyMute
                                SamsungGesturesManager.PALM_SWIPE -> palmSwipe
                                else -> smartStay
                            }
                        } else {
                            false // Non-device mock gestures default to OFF
                        }

                        val toggleHandler: (Boolean) -> Unit = { enabled ->
                            if (isDeviceSetting) {
                                when (gesture.samsungKey) {
                                    SamsungGesturesManager.WAKE_GESTURE -> toggleSamsungSetting(SamsungGesturesManager.WAKE_GESTURE, wakeGesture) { wakeGesture = it }
                                    SamsungGesturesManager.DOUBLE_TAP_ON -> toggleSamsungSetting(SamsungGesturesManager.DOUBLE_TAP_ON, doubleTapOn) { doubleTapOn = it }
                                    SamsungGesturesManager.DOUBLE_TAP_OFF -> toggleSamsungSetting(SamsungGesturesManager.DOUBLE_TAP_OFF, doubleTapOff) { doubleTapOff = it }
                                    SamsungGesturesManager.SMART_ALERT -> toggleSamsungSetting(SamsungGesturesManager.SMART_ALERT, smartAlert) { smartAlert = it }
                                    SamsungGesturesManager.EASY_MUTE -> toggleSamsungSetting(SamsungGesturesManager.EASY_MUTE, easyMute) { easyMute = it }
                                    SamsungGesturesManager.PALM_SWIPE -> toggleSamsungSetting(SamsungGesturesManager.PALM_SWIPE, palmSwipe) { palmSwipe = it }
                                    SamsungGesturesManager.SMART_STAY -> toggleSamsungSetting(SamsungGesturesManager.SMART_STAY, smartStay) { smartStay = it }
                                }
                            } else {
                                Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
                            }
                        }

                        val itemAlpha = if (!isDaemonActive) {
                            if (isDeviceSetting) 0.6f else 0.4f
                        } else {
                            if (isDeviceSetting) 1.0f else 0.6f
                        }

                        GestureRowItem(
                            title = gesture.title,
                            subtitle = gesture.description,
                            icon = gesture.icon,
                            isEnabled = switchState,
                            switchEnabled = isDaemonActive && isDeviceSetting,
                            alpha = itemAlpha,
                            onToggled = toggleHandler,
                            onClick = {
                                if (isDeviceSetting) {
                                    Toast.makeText(context, "Manage this gesture in device system settings.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Coming soon!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            } else if (searchQuery.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No gestures match \"$searchQuery\"",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GestureRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggled: (Boolean) -> Unit,
    onClick: () -> Unit,
    switchEnabled: Boolean = true,
    alpha: Float = 1.0f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .graphicsLayer(alpha = alpha),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggled,
            enabled = switchEnabled
        )
    }
}

fun getCardShape(style: OEMThemeStyle) = when (style) {
    OEMThemeStyle.ONE_UI -> RoundedCornerShape(26.dp)
    OEMThemeStyle.HYPER_OS -> RoundedCornerShape(18.dp)
    OEMThemeStyle.MOTO -> RoundedCornerShape(12.dp)
    OEMThemeStyle.MATERIAL_YOU -> RoundedCornerShape(16.dp)
}

// -----------------------------------------------------------------------------
// Component: Generic Gesture Detail Screen Wrapper
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericGestureDetailScreen(
    title: String,
    description: String,
    illustration: @Composable (Modifier) -> Unit,
    onNavigateBack: () -> Unit
) {
    var isGestureEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            illustration(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use $title",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Switch(checked = isGestureEnabled, onCheckedChange = { isGestureEnabled = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Interactive Illustrations for All Gestures
// -----------------------------------------------------------------------------
@Composable
fun QuickLaunchAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "quick_launch")
    val pulseScale by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "alpha"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        drawRoundRect(
            color = Color(0xFF2C3240),
            topLeft = Offset(cx - w * 0.15f, cy - h * 0.25f),
            size = Size(w * 0.3f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.04f, h * 0.04f)
        )

        val tapCenter = Offset(cx, cy - h * 0.05f)
        drawCircle(color = Color(0xFF0070F3).copy(alpha = pulseAlpha), radius = w * 0.15f * pulseScale, center = tapCenter)
        drawCircle(color = Color(0xFF0070F3), radius = w * 0.04f, center = tapCenter)
    }
}

@Composable
fun SidebarAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sidebar")
    val slideOffset by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "slide"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        val phoneW = w * 0.35f
        val phoneH = h * 0.5f
        val left = cx - phoneW / 2f
        val top = cy - phoneH / 2f

        drawRoundRect(color = Color(0xFF2C3240), topLeft = Offset(left, top), size = Size(phoneW, phoneH), cornerRadius = CornerRadius(w * 0.04f, h * 0.04f))

        val sidebarWidth = phoneW * 0.35f * slideOffset
        if (sidebarWidth > 2f) {
            drawRoundRect(
                color = Color(0xFF0070F3).copy(alpha = 0.8f),
                topLeft = Offset(left + phoneW - sidebarWidth, top + phoneH * 0.2f),
                size = Size(sidebarWidth, phoneH * 0.6f),
                cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
            )
        }
    }
}

@Composable
fun ThreeFingerAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "three_finger")
    val swipeY by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "swipeY"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        val phoneW = w * 0.35f
        val phoneH = h * 0.5f
        val left = cx - phoneW / 2f
        val top = cy - phoneH / 2f

        drawRoundRect(color = Color(0xFF2C3240), topLeft = Offset(left, top), size = Size(phoneW, phoneH), cornerRadius = CornerRadius(w * 0.04f, h * 0.04f))

        val fingerY = top + phoneH * swipeY
        val spacing = phoneW * 0.2f
        val f1 = Offset(cx - spacing, fingerY)
        val f2 = Offset(cx, fingerY)
        val f3 = Offset(cx + spacing, fingerY)

        drawCircle(color = Color(0xFF3DDB85), radius = w * 0.03f, center = f1)
        drawCircle(color = Color(0xFF3DDB85), radius = w * 0.03f, center = f2)
        drawCircle(color = Color(0xFF3DDB85), radius = w * 0.03f, center = f3)
    }
}

@Composable
fun LiftUnlockAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lift_unlock")
    val tiltAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = -25f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "tilt"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        rotate(tiltAngle, pivot = Offset(cx, cy)) {
            drawRoundRect(
                color = Color(0xFF2C3240),
                topLeft = Offset(cx - w * 0.15f, cy - h * 0.25f),
                size = Size(w * 0.3f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.04f, h * 0.04f)
            )
            drawCircle(color = Color(0xFF10B981), radius = w * 0.04f, center = Offset(cx, cy))
        }
    }
}

@Composable
fun FlipDndAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "flip_dnd")
    val flipScaleY by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = -1.0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "flip"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        scale(scaleX = 1.0f, scaleY = flipScaleY, pivot = Offset(cx, cy)) {
            drawRoundRect(
                color = Color(0xFF1F2937),
                topLeft = Offset(cx - w * 0.15f, cy - h * 0.25f),
                size = Size(w * 0.3f, h * 0.5f),
                cornerRadius = CornerRadius(w * 0.04f, h * 0.04f)
            )
        }
    }
}

@Composable
fun PickUpSilenceAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "silence")
    val waveScale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "wave"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.2f), radius = w * 0.2f * waveScale, center = Offset(cx, cy))
        drawCircle(color = Color(0xFFEF4444), radius = w * 0.08f, center = Offset(cx, cy))
    }
}

@Composable
fun SwipeSplitAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "swipe_split")
    val swipeX by transition.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "swipeX"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(color = Color.Black.copy(alpha = 0.05f), radius = size.minDimension * 0.4f, center = Offset(cx, cy))

        val phoneW = w * 0.35f
        val phoneH = h * 0.5f
        val left = cx - phoneW / 2f
        val top = cy - phoneH / 2f

        drawRoundRect(color = Color(0xFF2C3240), topLeft = Offset(left, top), size = Size(phoneW, phoneH), cornerRadius = CornerRadius(w * 0.04f, h * 0.04f))

        drawLine(color = Color(0xFF0070F3), start = Offset(left, cy), end = Offset(left + phoneW, cy), strokeWidth = w * 0.015f)

        drawCircle(color = Color.White, radius = w * 0.03f, center = Offset(cx + w * swipeX, cy))
    }
}

// -----------------------------------------------------------------------------
// Component: Fast Torch Detail Screen
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTorchDetailScreen(
    isChopEnabled: Boolean,
    onChopToggled: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateTryIt: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ChopChopAnimation(
                isTorchActive = isChopEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fast torch",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Turn the torch on/off with two karate chop motions",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Fast torch",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Switch(checked = isChopEnabled, onCheckedChange = onChopToggled)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier
                        .size(54.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onNavigateTryIt,
                    shape = RoundedCornerShape(27.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Try it out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun ChopChopAnimation(isTorchActive: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "chop")

    val angle by transition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "angle"
    )

    val beamPulse by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beamPulse"
    )

    var showLight by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1800)
            showLight = !showLight
        }
    }

    val isLightGlowing = isTorchActive && showLight

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(
            color = Color.Black.copy(alpha = 0.05f),
            radius = size.minDimension * 0.4f,
            center = Offset(cx, cy)
        )

        rotate(angle, pivot = Offset(cx, cy)) {
            drawLine(
                color = Color(0xFFD2A384),
                start = Offset(cx - w * 0.1f, cy + h * 0.3f),
                end = Offset(cx - w * 0.15f, cy + h * 0.1f),
                strokeWidth = w * 0.12f,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = Color(0xFFD2A384),
                radius = w * 0.08f,
                center = Offset(cx - w * 0.06f, cy + h * 0.05f)
            )

            drawRoundRect(
                color = Color(0xFF2C3240),
                topLeft = Offset(cx - w * 0.05f, cy - h * 0.25f),
                size = Size(w * 0.18f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
            )

            drawRoundRect(
                color = Color(0xFF161A22),
                topLeft = Offset(cx - w * 0.04f, cy - h * 0.23f),
                size = Size(w * 0.16f, h * 0.38f),
                cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
            )

            drawCircle(
                color = Color(0xFF101216),
                radius = w * 0.025f,
                center = Offset(cx + w * 0.04f, cy - h * 0.18f)
            )

            if (isLightGlowing) {
                drawCircle(
                    color = Color(0xFFFFF0B3),
                    radius = w * 0.015f,
                    center = Offset(cx + w * 0.04f, cy - h * 0.18f)
                )

                val beamPath = Path().apply {
                    moveTo(cx + w * 0.04f, cy - h * 0.18f)
                    lineTo(cx + w * 0.5f, cy - h * 0.38f)
                    lineTo(cx + w * 0.7f, cy - h * 0.1f)
                    close()
                }

                drawPath(
                    path = beamPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFF0B3).copy(alpha = 0.8f * beamPulse),
                            Color(0xFFFFEA7A).copy(alpha = 0.3f * beamPulse),
                            Color.Transparent
                        ),
                        center = Offset(cx + w * 0.04f, cy - h * 0.18f),
                        radius = w * 0.6f
                    )
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Fast Torch Settings Screen
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastTorchSettingsScreen(
    isVibrationEnabled: Boolean,
    onVibrationToggled: (Boolean) -> Unit,
    chopSensitivity: Float,
    onSensitivityChanged: (Float) -> Unit,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vibration",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Switch(checked = isVibrationEnabled, onCheckedChange = onVibrationToggled)
                    }
                    Text(
                        text = "Vibrate when torch turns on/off",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Karate Chop Sensitivity",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${String.format("%.1f", chopSensitivity)} m/s²",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = chopSensitivity,
                        onValueChange = { valNew ->
                            if (kotlin.math.abs(valNew - chopSensitivity) >= 0.5f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSensitivityChanged(valNew)
                        },
                        valueRange = 15.0f..45.0f,
                        steps = 5
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("15.0 (Sensitive)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("30.0 (Default)", fontSize = 11.sp, color = Color.Gray)
                        Text("45.0 (Firm Chop)", fontSize = 11.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("◄ Sliding Left (Lower): Sensitive trigger (Easy karate chop; light shake can turn on torch)", fontSize = 11.sp, lineHeight = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("► Sliding Right (Higher): Firm trigger (Requires forceful chop; prevents accidental triggers)", fontSize = 11.sp, lineHeight = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "To use this feature, vibration and haptics must be enabled on your device. You can enable this in System settings.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Quick Capture Detail Screen
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureDetailScreen(
    isTwistEnabled: Boolean,
    onTwistToggled: (Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateSettings: () -> Unit,
    onNavigateTryIt: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuickCaptureAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Quick capture",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Twist your wrist twice quickly to open the camera at any time",
                fontSize = 15.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Quick capture",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    Switch(checked = isTwistEnabled, onCheckedChange = onTwistToggled)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateSettings,
                    modifier = Modifier
                        .size(54.dp)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onNavigateTryIt,
                    shape = RoundedCornerShape(27.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Try it out", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickCaptureAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "twist")

    val scaleX by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = -1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleX"
    )

    val angle by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "angle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        drawCircle(
            color = Color.Black.copy(alpha = 0.05f),
            radius = size.minDimension * 0.4f,
            center = Offset(cx, cy)
        )

        rotate(angle, pivot = Offset(cx, cy)) {
            drawLine(
                color = Color(0xFFD2A384),
                start = Offset(cx - w * 0.05f, cy + h * 0.3f),
                end = Offset(cx - w * 0.08f, cy + h * 0.1f),
                strokeWidth = w * 0.1f,
                cap = StrokeCap.Round
            )

            drawCircle(
                color = Color(0xFFD2A384),
                radius = w * 0.07f,
                center = Offset(cx - w * 0.04f, cy + h * 0.05f)
            )

            scale(scaleX = scaleX, scaleY = 1.0f, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    color = Color(0xFF2C3240),
                    topLeft = Offset(cx - w * 0.1f, cy - h * 0.2f),
                    size = Size(w * 0.2f, h * 0.4f),
                    cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
                )

                drawRoundRect(
                    color = Color(0xFF0070F3),
                    topLeft = Offset(cx - w * 0.09f, cy - h * 0.18f),
                    size = Size(w * 0.18f, h * 0.36f),
                    cornerRadius = CornerRadius(w * 0.02f, h * 0.02f)
                )

                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = w * 0.04f,
                    center = Offset(cx, cy - h * 0.05f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.02f,
                    center = Offset(cx, cy - h * 0.05f)
                )

                drawCircle(
                    color = Color.Black,
                    radius = w * 0.012f,
                    center = Offset(cx, cy - h * 0.15f)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Quick Capture Settings Screen
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSettingsScreen(
    cameraOption: String,
    onCameraOptionSelected: (String) -> Unit,
    twistSensitivity: Float,
    onSensitivityChanged: (Float) -> Unit,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Choose which camera to open when you twist",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCameraOptionSelected("auto") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = cameraOption == "auto", onClick = { onCameraOptionSelected("auto") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Auto", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text(
                                    "Twisting in portrait opens selfie camera; in landscape opens rear camera",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCameraOptionSelected("rear") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = cameraOption == "rear", onClick = { onCameraOptionSelected("rear") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Rear", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Twisting always opens the rear camera", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCameraOptionSelected("front") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = cameraOption == "front", onClick = { onCameraOptionSelected("front") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Front", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Twisting always opens the selfie camera", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Double Twist Angular Velocity",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${String.format("%.1f", twistSensitivity)} rad/s",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = twistSensitivity,
                        onValueChange = { valNew ->
                            if (kotlin.math.abs(valNew - twistSensitivity) >= 0.3f) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onSensitivityChanged(valNew)
                        },
                        valueRange = 4.0f..16.0f,
                        steps = 5
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("4.0 (Sensitive)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        Text("8.5 (Default)", fontSize = 11.sp, color = Color.Gray)
                        Text("16.0 (Firm Twist)", fontSize = 11.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Column {
                            Text("◄ Sliding Left (Lower): Sensitive wrist turn (Opens camera with light wrist angle)", fontSize = 11.sp, lineHeight = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("► Sliding Right (Higher): Firm double twist (Requires snappy twist; prevents accidental launches)", fontSize = 11.sp, lineHeight = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Try It Out Screen
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TryItScreen(
    title: String,
    instructions: String,
    hint: String,
    successMsg: String,
    gestureId: String,
    settings: SettingsManager,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var isSuccess by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val gestureEngine = GestureEngine(context)
        val detector = if (gestureId == "chop_chop") {
            ChopGestureDetector(settings.chopSensitivity) {
                isSuccess = true
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(150)
                    }
                }
            }
        } else {
            TwistGestureDetector(settings.twistSensitivity) {
                isSuccess = true
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(150)
                    }
                }
            }
        }

        gestureEngine.registerDetector(detector)
        gestureEngine.startListening()

        onDispose {
            gestureEngine.stopListening()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Room", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = instructions,
                    fontSize = 15.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSuccess) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.5f
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSuccess) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(96.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (gestureId == "chop_chop") Icons.Default.FlashlightOn else Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (isSuccess) successMsg else hint,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            if (isSuccess) {
                Button(
                    onClick = { isSuccess = false },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Try again", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Make the physical motions with your phone now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Settings Screen (Renamed from Personalise + Sensitivity Dialers)
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    onThemeModeSelected: (String) -> Unit,
    themeStyle: String,
    onThemeStyleSelected: (String) -> Unit,
    chopSensitivity: Float,
    onChopSensitivityChanged: (Float) -> Unit,
    twistSensitivity: Float,
    onTwistSensitivityChanged: (Float) -> Unit,
    onNavigateBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Sensitivity Dialers Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Gesture Sensitivity Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    // Chop Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fast Torch (Karate Chop)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${String.format("%.1f", chopSensitivity)} m/s²", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = chopSensitivity,
                            onValueChange = { valNew ->
                                if (kotlin.math.abs(valNew - chopSensitivity) >= 0.5f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onChopSensitivityChanged(valNew)
                            },
                            valueRange = 15.0f..45.0f,
                            steps = 5
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("15.0 (Sensitive)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("30.0 (Default)", fontSize = 11.sp, color = Color.Gray)
                            Text("45.0 (Firm Chop)", fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Column {
                                Text("◄ Sliding Left (Lower): Sensitive trigger (Easy karate chop; light shake can turn on torch)", fontSize = 11.sp, lineHeight = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("► Sliding Right (Higher): Firm trigger (Requires forceful chop; prevents accidental triggers)", fontSize = 11.sp, lineHeight = 14.sp, color = Color.Gray)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    // Twist Sensitivity Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Quick Capture (Double Twist)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("${String.format("%.1f", twistSensitivity)} rad/s", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = twistSensitivity,
                            onValueChange = { valNew ->
                                if (kotlin.math.abs(valNew - twistSensitivity) >= 0.3f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onTwistSensitivityChanged(valNew)
                            },
                            valueRange = 4.0f..16.0f,
                            steps = 5
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("4.0 (Sensitive)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("8.5 (Default)", fontSize = 11.sp, color = Color.Gray)
                            Text("16.0 (Firm Twist)", fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Column {
                                Text("◄ Sliding Left (Lower): Sensitive wrist turn (Opens camera with light wrist angle)", fontSize = 11.sp, lineHeight = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("► Sliding Right (Higher): Firm double twist (Requires snappy twist; prevents accidental launches)", fontSize = 11.sp, lineHeight = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Theme Mode selection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeSelected("system") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == "system", onClick = { onThemeModeSelected("system") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("System Default", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeSelected("dark") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == "dark", onClick = { onThemeModeSelected("dark") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dark Mode", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeModeSelected("light") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == "light", onClick = { onThemeModeSelected("light") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Light Mode", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Theme Style selection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Theme design style",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeStyleSelected("device") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeStyle == "device", onClick = { onThemeStyleSelected("device") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Device Adaptive UI", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Resolves theme style based on system manufacturer", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeStyleSelected("material") }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeStyle == "material", onClick = { onThemeStyleSelected("material") })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Material You / M3", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Standard Google Material design with Monet support", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Dedicated Support Screen Page
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    activeStyle: OEMThemeStyle,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support Developer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCE4EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(48.dp)
                )
            }

            Text(
                text = "Support GestureActions",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Made with 🐾 by technoerd",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "GestureActions is built to bring responsive karate chop flashlight & twist camera gestures to Samsung and Android devices. Your support helps maintain low-latency sensor algorithms, hardware fixes, and ad-free utility development! 🐱",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Developer Support Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Button(
                        onClick = {
                            Toast.makeText(context, "Thank you for the Coffee tip! Support Kit ready.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null)
                            Text("Buy a Coffee ($1.99)", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            Toast.makeText(context, "Cat Treats Tip sent! Thank you from technoerd & cat! 🐾", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Buy Cat Treats 🐱 ($4.99)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Thank you for supporting independent developer technoerd!",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// -----------------------------------------------------------------------------
// Component: About Screen
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    activeStyle: OEMThemeStyle,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            BrandLogo(style = activeStyle, size = 96.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GestureActions",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
            Text(
                text = "Version 1.0.0",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "GestureActions is a high-performance utility that brings Moto-style Chop Chop and Quick Twist gestures to Samsung, Xiaomi, and other Android devices. Built using hardware-fused sensors and low-latency system hooks to deliver maximum reliability and minimal battery impact.",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Developer", color = Color.Gray, fontSize = 14.sp)
                        Text("technoerd", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Copyright", color = Color.Gray, fontSize = 14.sp)
                        Text("© 2026 technoerd", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compatibility", color = Color.Gray, fontSize = 14.sp)
                        Text("Android 7.0+", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Made with 🐾 by technoerd",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
