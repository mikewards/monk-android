package com.monk.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monk.app.domain.model.SupportedApp
import com.monk.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    onNavigateBack: () -> Unit
) {
    // Track which apps are enabled
    var enabledApps by remember { 
        mutableStateOf(SupportedApp.defaultEnabled().map { it.packageName }.toSet())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enabled Apps", fontWeight = FontWeight.Bold) },
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
        ) {
            // Info text
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Primary.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "Select which apps should receive your auto-reply message when you're in focus mode.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = PrimaryVariant
                )
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SupportedApp.entries) { app ->
                    AppItem(
                        app = app,
                        isEnabled = enabledApps.contains(app.packageName),
                        onToggle = { enabled ->
                            enabledApps = if (enabled) {
                                enabledApps + app.packageName
                            } else {
                                enabledApps - app.packageName
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppItem(
    app: SupportedApp,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon placeholder (emoji for now)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Gray100,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = getAppEmoji(app),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray500
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = Primary
                )
            )
        }
    }
}

private fun getAppEmoji(app: SupportedApp): String {
    return when (app) {
        SupportedApp.WHATSAPP, SupportedApp.WHATSAPP_BUSINESS -> "💬"
        SupportedApp.MESSENGER, SupportedApp.MESSENGER_LITE -> "💙"
        SupportedApp.INSTAGRAM -> "📷"
        SupportedApp.TELEGRAM, SupportedApp.TELEGRAM_X -> "✈️"
        SupportedApp.SIGNAL -> "🔒"
        SupportedApp.MESSAGES, SupportedApp.SAMSUNG_MESSAGES -> "💬"
        SupportedApp.DISCORD -> "🎮"
        SupportedApp.SLACK -> "💼"
        SupportedApp.TWITTER -> "🐦"
        SupportedApp.SNAPCHAT -> "👻"
    }
}
