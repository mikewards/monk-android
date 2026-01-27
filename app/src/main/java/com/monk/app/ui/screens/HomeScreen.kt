package com.monk.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.monk.app.data.datastore.PreferencesManager
import com.monk.app.domain.model.CustomDuration
import com.monk.app.domain.model.FocusDuration
import com.monk.app.domain.model.FocusState
import com.monk.app.ui.theme.*
import com.monk.app.util.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    val focusState by viewModel.focusState.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()
    
    var permissionStatus by remember { mutableStateOf(PermissionHelper.getPermissionStatus(context)) }
    var showNotificationHint by remember { mutableStateOf(false) }
    var hintDismissed by remember { mutableStateOf(true) }
    var lastFocusWasActive by remember { mutableStateOf(false) }
    
    // Load hint dismissed state
    LaunchedEffect(Unit) {
        hintDismissed = preferencesManager.notificationHintDismissed.first()
    }
    
    // Track focus session end with 0 replies
    LaunchedEffect(focusState.isActive) {
        if (lastFocusWasActive && !focusState.isActive) {
            // Focus just ended
            if (focusState.repliesSent == 0 && focusState.notificationsSilenced == 0 && !hintDismissed) {
                showNotificationHint = true
            }
        }
        lastFocusWasActive = focusState.isActive
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionStatus = PermissionHelper.getPermissionStatus(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar - Minimal
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONK",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 4.sp,
                    color = TextMuted
                )
                
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Permission Warning
            if (!permissionStatus.requiredPermissionsGranted) {
                Spacer(modifier = Modifier.height(24.dp))
                SetupRequiredCard(onClick = onNavigateToOnboarding)
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Main Focus Control
            FocusOrb(
                isActive = focusState.isActive,
                isEnabled = permissionStatus.requiredPermissionsGranted,
                onClick = { 
                    if (permissionStatus.requiredPermissionsGranted) {
                        if (focusState.isActive) {
                            viewModel.stopFocus()
                        } else {
                            viewModel.startFocus()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Status
            Text(
                text = when {
                    !permissionStatus.requiredPermissionsGranted -> "Setup required"
                    focusState.isActive -> "Focus active"
                    else -> "Ready"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = when {
                    !permissionStatus.requiredPermissionsGranted -> Warning
                    focusState.isActive -> Primary
                    else -> TextMuted
                },
                letterSpacing = 1.sp
            )

            // Timer
            if (focusState.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                TimerDisplay(focusState = focusState)
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Duration Selector
            if (!focusState.isActive) {
                var showCustomDialog by remember { mutableStateOf(false) }
                
                DurationSelector(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { duration ->
                        if (duration.isCustom) {
                            showCustomDialog = true
                        } else {
                            viewModel.setDuration(duration)
                        }
                    }
                )
                
                if (showCustomDialog) {
                    CustomDurationDialog(
                        initialMinutes = CustomDuration.minutes,
                        onDismiss = { showCustomDialog = false },
                        onConfirm = { minutes ->
                            CustomDuration.minutes = minutes
                            viewModel.setDuration(FocusDuration.CUSTOM)
                            showCustomDialog = false
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Stats - Minimal
            if (focusState.isActive || focusState.notificationsSilenced > 0 || focusState.repliesSent > 0) {
                StatsRow(focusState = focusState)
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Notification hint - shows after focus session with 0 activity
            if (showNotificationHint && !hintDismissed) {
                NotificationHint(
                    onOpenSettings = { 
                        PermissionHelper.openAppNotificationSettings(context)
                    },
                    onDismiss = {
                        showNotificationHint = false
                        hintDismissed = true
                        scope.launch {
                            preferencesManager.setNotificationHintDismissed(true)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SetupRequiredCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = WarningLight,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Complete setup to begin",
                style = MaterialTheme.typography.bodyMedium,
                color = Warning,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge,
                color = Warning
            )
        }
    }
}

@Composable
private fun FocusOrb(
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    
    // Subtle pulse when active
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    // Glow animation when active
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = if (isActive) 0.25f else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Gray200
            isActive -> Primary
            else -> SurfaceElevated
        },
        animationSpec = tween(500),
        label = "bg"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !isEnabled -> Gray300
            isActive -> Primary
            else -> Border
        },
        animationSpec = tween(500),
        label = "border"
    )

    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer glow (only when active)
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(pulseScale)
                    .alpha(glowAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(Accent, Accent.copy(alpha = 0f))
                        ),
                        shape = CircleShape
                    )
            )
        }

        // Main orb
        Box(
            modifier = Modifier
                .size(180.dp)
                .scale(if (isActive) pulseScale else 1f)
                .clip(CircleShape)
                .background(backgroundColor)
                .border(1.dp, borderColor, CircleShape)
                .clickable(
                    enabled = isEnabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when {
                        !isEnabled -> "○"
                        isActive -> "◉"
                        else -> "○"
                    },
                    fontSize = 48.sp,
                    color = when {
                        !isEnabled -> Gray400
                        isActive -> Accent
                        else -> Primary
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when {
                        !isEnabled -> "LOCKED"
                        isActive -> "END"
                        else -> "BEGIN"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 3.sp,
                    color = when {
                        !isEnabled -> Gray500
                        isActive -> OnPrimary
                        else -> Primary
                    }
                )
            }
        }
    }
}

@Composable
private fun TimerDisplay(focusState: FocusState) {
    val duration = focusState.duration
    val timeRemaining = focusState.timeRemaining

    val displayText = when {
        timeRemaining != null -> {
            val hours = timeRemaining.toHours()
            val minutes = timeRemaining.toMinutes() % 60
            val seconds = timeRemaining.seconds % 60
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
        duration != null -> {
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            if (hours > 0) {
                String.format("%d:%02d:00", hours, minutes)
            } else {
                String.format("%02d:00", minutes)
            }
        }
        else -> "∞"
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Light,
        color = Primary,
        letterSpacing = 2.sp
    )
}

@Composable
private fun DurationSelector(
    selectedDuration: FocusDuration,
    onDurationSelected: (FocusDuration) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DURATION",
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            color = TextMuted
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusDuration.entries.forEach { duration ->
                val displayText = if (duration.isCustom && duration == selectedDuration) {
                    val mins = CustomDuration.minutes
                    if (mins >= 60) {
                        "${mins / 60}h${if (mins % 60 > 0) " ${mins % 60}m" else ""}"
                    } else {
                        "${mins}m"
                    }
                } else {
                    duration.displayName
                }
                
                DurationPill(
                    text = displayText,
                    isSelected = duration == selectedDuration,
                    onClick = { onDurationSelected(duration) }
                )
            }
        }
    }
}

@Composable
private fun DurationPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Primary else SurfaceElevated,
        animationSpec = tween(200),
        label = "pill_bg"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) OnPrimary else TextMuted,
        animationSpec = tween(200),
        label = "pill_text"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = if (!isSelected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = textColor
        )
    }
}

@Composable
private fun StatsRow(focusState: FocusState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            value = "${focusState.notificationsSilenced}",
            label = "silenced"
        )
        
        Spacer(modifier = Modifier.width(48.dp))
        
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(32.dp)
                .background(Divider)
        )
        
        Spacer(modifier = Modifier.width(48.dp))
        
        StatItem(
            value = "${focusState.repliesSent}",
            label = "replies"
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Light,
            color = Primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun NotificationHint(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No messages?",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Primary,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✕",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Make sure notifications are enabled for your apps.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            TextButton(
                onClick = onOpenSettings,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Open notification settings →",
                    style = MaterialTheme.typography.labelMedium,
                    color = Primary
                )
            }
        }
    }
}

@Composable
private fun CustomDurationDialog(
    initialMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var hours by remember { mutableStateOf(initialMinutes / 60) }
    var minutes by remember { mutableStateOf(initialMinutes % 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = "Set Duration",
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hours
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { if (hours < 12) hours++ }) {
                            Text("▲", color = Primary, fontSize = 20.sp)
                        }
                        Text(
                            text = "$hours",
                            style = MaterialTheme.typography.displaySmall,
                            color = Primary
                        )
                        IconButton(onClick = { if (hours > 0) hours-- }) {
                            Text("▼", color = Primary, fontSize = 20.sp)
                        }
                        Text("hours", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.displaySmall,
                        color = Primary
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    // Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { minutes = (minutes + 15) % 60 }) {
                            Text("▲", color = Primary, fontSize = 20.sp)
                        }
                        Text(
                            text = String.format("%02d", minutes),
                            style = MaterialTheme.typography.displaySmall,
                            color = Primary
                        )
                        IconButton(onClick = { minutes = if (minutes >= 15) minutes - 15 else 45 }) {
                            Text("▼", color = Primary, fontSize = 20.sp)
                        }
                        Text("min", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val totalMinutes = hours * 60 + minutes
                    if (totalMinutes > 0) {
                        onConfirm(totalMinutes)
                    }
                },
                enabled = hours > 0 || minutes > 0
            ) {
                Text("Set", color = Primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}
