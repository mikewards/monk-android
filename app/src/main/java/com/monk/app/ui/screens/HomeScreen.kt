package com.monk.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.monk.app.domain.model.FocusDuration
import com.monk.app.domain.model.FocusState
import com.monk.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToApps: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val focusState by viewModel.focusState.collectAsState()
    val selectedDuration by viewModel.selectedDuration.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Monk",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Main Focus Toggle Button
            FocusToggleButton(
                isActive = focusState.isActive,
                onClick = { 
                    if (focusState.isActive) {
                        viewModel.stopFocus()
                    } else {
                        viewModel.startFocus()
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text
            Text(
                text = if (focusState.isActive) "Focus Mode Active" else "Tap to Enter Focus Mode",
                style = MaterialTheme.typography.titleMedium,
                color = if (focusState.isActive) Primary else Gray500
            )

            // Timer Display (when active)
            if (focusState.isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                TimerDisplay(focusState = focusState)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Duration Selector (when not active)
            if (!focusState.isActive) {
                DurationSelector(
                    selectedDuration = selectedDuration,
                    onDurationSelected = { viewModel.setDuration(it) }
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Notifications,
                    value = "${focusState.notificationsSilenced}",
                    label = "Silenced"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Send,
                    value = "${focusState.repliesSent}",
                    label = "Replies Sent"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Quick Actions
            OutlinedButton(
                onClick = onNavigateToApps,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Apps, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Manage Apps")
            }
        }
    }
}

@Composable
private fun FocusToggleButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isActive) Primary else Gray200,
        animationSpec = tween(300),
        label = "bg_color"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.05f else 1f,
        animationSpec = tween(300),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🧘",
                fontSize = 64.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isActive) "END" else "START",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isActive) OnPrimary else Gray700
            )
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
            val seconds = duration.seconds % 60
            if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
        else -> "∞"
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = Primary
    )
}

@Composable
private fun DurationSelector(
    selectedDuration: FocusDuration,
    onDurationSelected: (FocusDuration) -> Unit
) {
    Column {
        Text(
            text = "Session Duration",
            style = MaterialTheme.typography.titleSmall,
            color = Gray500,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusDuration.entries.forEach { duration ->
                DurationChip(
                    duration = duration,
                    isSelected = duration == selectedDuration,
                    onClick = { onDurationSelected(duration) }
                )
            }
        }
    }
}

@Composable
private fun DurationChip(
    duration: FocusDuration,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) Primary else Gray100,
        contentColor = if (isSelected) OnPrimary else Gray700
    ) {
        Text(
            text = duration.displayName,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Gray500
            )
        }
    }
}

@Composable
private fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simple flow row implementation
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() }
    )
}
