package com.monk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monk.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToContacts: () -> Unit
) {
    var replyMessage by remember { mutableStateOf("Hey! I'm currently in focus mode and can't respond right now. I'll get back to you soon! 🧘") }
    var cooldownMinutes by remember { mutableStateOf(5f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reply Message
            SettingsSection(title = "Auto-Reply Message") {
                OutlinedTextField(
                    value = replyMessage,
                    onValueChange = { replyMessage = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Cooldown
            SettingsSection(title = "Reply Cooldown") {
                Column {
                    Text(
                        text = "${cooldownMinutes.toInt()} minutes",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Time before replying to the same person again",
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray500
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = cooldownMinutes,
                        onValueChange = { cooldownMinutes = it },
                        valueRange = 1f..60f,
                        steps = 58
                    )
                }
            }

            // Quick Links
            SettingsSection(title = "Configuration") {
                SettingsItem(
                    icon = Icons.Outlined.Apps,
                    title = "Enabled Apps",
                    subtitle = "Choose which apps receive auto-replies",
                    onClick = onNavigateToApps
                )
                SettingsItem(
                    icon = Icons.Outlined.People,
                    title = "Whitelisted Contacts",
                    subtitle = "Contacts who can still reach you",
                    onClick = onNavigateToContacts
                )
            }

            // Permissions
            SettingsSection(title = "Permissions") {
                SettingsItem(
                    icon = Icons.Outlined.Notifications,
                    title = "Notification Access",
                    subtitle = "Required to read incoming messages",
                    onClick = { /* Open notification settings */ }
                )
                SettingsItem(
                    icon = Icons.Outlined.Accessibility,
                    title = "Accessibility Access",
                    subtitle = "Required to send auto-replies",
                    onClick = { /* Open accessibility settings */ }
                )
            }

            // About
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = { }
                )
                SettingsItem(
                    icon = Icons.Outlined.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    onClick = { }
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
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
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Gray400
            )
        }
    }
}
