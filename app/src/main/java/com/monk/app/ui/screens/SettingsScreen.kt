package com.monk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.monk.app.data.datastore.PreferencesManager
import com.monk.app.service.AutoReplyService
import com.monk.app.service.FocusService
import com.monk.app.service.NotificationListener
import com.monk.app.ui.theme.*
import com.monk.app.util.PermissionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToContacts: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    // Load saved values from DataStore
    var replyMessage by remember { mutableStateOf("") }
    var cooldownMinutes by remember { mutableStateOf(5f) }
    var deepFocusEnabled by remember { mutableStateOf(false) }
    var dndEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var permissionStatus by remember { mutableStateOf(PermissionHelper.getPermissionStatus(context)) }

    // Load initial values
    LaunchedEffect(Unit) {
        replyMessage = preferencesManager.replyMessage.first()
        cooldownMinutes = preferencesManager.replyCooldownMinutes.first().toFloat()
        deepFocusEnabled = preferencesManager.deepFocusEnabled.first()
        dndEnabled = preferencesManager.dndEnabled.first()
        isLoading = false
    }

    // Save reply message when it changes
    LaunchedEffect(replyMessage) {
        if (!isLoading && replyMessage.isNotEmpty()) {
            preferencesManager.setReplyMessage(replyMessage)
            NotificationListener.replyMessage = replyMessage
        }
    }

    // Save cooldown when it changes
    LaunchedEffect(cooldownMinutes) {
        if (!isLoading) {
            preferencesManager.setReplyCooldownMinutes(cooldownMinutes.toInt())
            NotificationListener.cooldownMinutes = cooldownMinutes.toInt()
        }
    }

    // Save deep focus when it changes
    LaunchedEffect(deepFocusEnabled) {
        if (!isLoading) {
            preferencesManager.setDeepFocusEnabled(deepFocusEnabled)
            FocusService.deepFocusEnabled = deepFocusEnabled
            // Also update live service if focus is currently running
            if (FocusService.isRunning) {
                AutoReplyService.deepFocusActive = deepFocusEnabled
            }
        }
    }

    // Save DND when it changes
    LaunchedEffect(dndEnabled) {
        if (!isLoading) {
            preferencesManager.setDndEnabled(dndEnabled)
            FocusService.dndEnabled = dndEnabled
        }
    }

    // Auto-enable features when user grants permissions
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val newStatus = PermissionHelper.getPermissionStatus(context)
                
                // If accessibility was just granted and deep focus isn't on yet, turn it on
                val accessibilityWasDisabled = !permissionStatus.accessibilityService
                val accessibilityNowEnabled = newStatus.accessibilityService
                if (accessibilityNowEnabled && accessibilityWasDisabled && !deepFocusEnabled) {
                    deepFocusEnabled = true
                    scope.launch {
                        preferencesManager.setDeepFocusEnabled(true)
                        FocusService.deepFocusEnabled = true
                        if (FocusService.isRunning) {
                            AutoReplyService.deepFocusActive = true
                        }
                    }
                }
                
                // If DND access was just granted and DND isn't on yet, turn it on
                val dndWasDisabled = !permissionStatus.dndAccess
                val dndNowEnabled = newStatus.dndAccess
                if (dndNowEnabled && dndWasDisabled && !dndEnabled) {
                    dndEnabled = true
                    scope.launch {
                        preferencesManager.setDndEnabled(true)
                        FocusService.dndEnabled = true
                    }
                }
                
                permissionStatus = newStatus
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        "← Back",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary
                    )
                }
                
                Text(
                    text = "SETTINGS",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 3.sp,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.width(64.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Setup Required Card
            if (!permissionStatus.requiredPermissionsGranted) {
                SetupRequiredCard(
                    onClick = { PermissionHelper.openNotificationListenerSettings(context) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Reply Message
            SettingsSection(
                title = "AUTO-REPLY",
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                OutlinedTextField(
                    value = replyMessage,
                    onValueChange = { replyMessage = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Border,
                        focusedBorderColor = Primary
                    ),
                    enabled = !isLoading
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cooldown
            SettingsSection(
                title = "COOLDOWN",
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${cooldownMinutes.toInt()} min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Between replies to same person",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = cooldownMinutes,
                        onValueChange = { cooldownMinutes = it },
                        valueRange = 1f..60f,
                        steps = 58,
                        colors = SliderDefaults.colors(
                            thumbColor = Primary,
                            activeTrackColor = Primary,
                            inactiveTrackColor = Gray200
                        ),
                        enabled = !isLoading
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Focus Mode Settings
            SettingsSection(
                title = "FOCUS MODE",
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Do Not Disturb toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Silence Phone",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Primary
                        )
                        Text(
                            text = if (permissionStatus.dndAccess)
                                "Enable Do Not Disturb during focus"
                            else
                                "Tap to grant DND access",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionStatus.dndAccess) TextMuted else Warning
                        )
                    }
                    Switch(
                        checked = dndEnabled && permissionStatus.dndAccess,
                        onCheckedChange = { enabled ->
                            if (enabled && !permissionStatus.dndAccess) {
                                PermissionHelper.openDndAccessSettings(context)
                            } else {
                                dndEnabled = enabled
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceElevated,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = Gray400,
                            uncheckedTrackColor = Gray200
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Divider)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Deep Focus toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stay in Monk",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Primary
                        )
                        Text(
                            text = if (permissionStatus.accessibilityService)
                                "Brings you back if you try to leave"
                            else
                                "Tap to grant Accessibility access",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionStatus.accessibilityService) TextMuted else Warning
                        )
                    }
                    Switch(
                        checked = deepFocusEnabled && permissionStatus.accessibilityService,
                        onCheckedChange = { enabled ->
                            if (enabled && !permissionStatus.accessibilityService) {
                                PermissionHelper.openAccessibilitySettings(context)
                            } else {
                                deepFocusEnabled = enabled
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SurfaceElevated,
                            checkedTrackColor = Primary,
                            uncheckedThumbColor = Gray400,
                            uncheckedTrackColor = Gray200
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Configuration
            SettingsSection(
                title = "CONFIGURE",
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                SettingsRow(
                    title = "Enabled Apps",
                    subtitle = "Apps that receive auto-replies",
                    onClick = onNavigateToApps
                )
                Divider(color = Divider)
                SettingsRow(
                    title = "Whitelisted Contacts",
                    subtitle = "Always allowed to reach you",
                    onClick = onNavigateToContacts
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Permissions
            SettingsSection(
                title = "PERMISSIONS",
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                PermissionRow(
                    title = "Notification Access",
                    subtitle = if (permissionStatus.notificationListener) "Enabled" else "Required to work",
                    isGranted = permissionStatus.notificationListener,
                    onClick = { PermissionHelper.openNotificationListenerSettings(context) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Note
            PrivacyNote(modifier = Modifier.padding(horizontal = 24.dp))

            Spacer(modifier = Modifier.height(32.dp))

            // Version
            Text(
                text = "Monk v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = TextHint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun SetupRequiredCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = WarningLight,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Setup Required",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Warning
                )
                Text(
                    text = "Tap to grant notification access",
                    style = MaterialTheme.typography.bodySmall,
                    color = Warning.copy(alpha = 0.8f)
                )
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.headlineSmall,
                color = Warning
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceElevated,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Primary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String = "",
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Primary
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (!isGranted && subtitle == "Required to work") Warning else TextMuted
                    )
                }
            }
            Text(
                text = "→",
                style = MaterialTheme.typography.bodyLarge,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun PrivacyNote(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Primary.copy(alpha = 0.05f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Your Privacy",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = Primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Monk works completely offline. Your messages stay on your phone — we never see them.",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                lineHeight = 18.sp
            )
        }
    }
}
