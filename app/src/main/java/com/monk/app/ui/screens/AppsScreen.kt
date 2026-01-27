package com.monk.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monk.app.domain.model.SupportedApp
import com.monk.app.ui.theme.*
import com.monk.app.util.PermissionHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var enabledApps by remember { 
        mutableStateOf(SupportedApp.entries.map { it.packageName }.toSet())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
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
                    text = "APPS",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 3.sp,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.width(64.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "Select apps that will receive auto-replies during focus mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Helper text about notifications
            Surface(
                onClick = { PermissionHelper.openAppNotificationSettings(context) },
                shape = RoundedCornerShape(8.dp),
                color = Primary.copy(alpha = 0.05f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-reply only works for apps with notifications enabled.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "→",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apps List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SupportedApp.entries) { app ->
                    AppToggleItem(
                        app = app,
                        isEnabled = app.packageName in enabledApps,
                        onToggle = { enabled ->
                            enabledApps = if (enabled) {
                                enabledApps + app.packageName
                            } else {
                                enabledApps - app.packageName
                            }
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun AppToggleItem(
    app: SupportedApp,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Primary
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextHint
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceElevated,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = Gray400,
                    uncheckedTrackColor = Gray200
                )
            )
        }
    }
}
