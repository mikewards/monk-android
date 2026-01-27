package com.monk.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.monk.app.ui.theme.*
import com.monk.app.util.PermissionHelper
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val buttonText: String,
    val permissionType: PermissionType = PermissionType.NONE,
    val isRequired: Boolean = true,
    val isPrivacyPage: Boolean = false,
    val privacyFeatures: List<String> = emptyList()
)

enum class PermissionType {
    NONE,
    NOTIFICATION_LISTENER,
    ACCESSIBILITY,
    RUNTIME_PERMISSIONS
}

private val onboardingPages = listOf(
    OnboardingPage(
        title = "Monk",
        description = "Silence distractions.\nReply automatically.\nBe present.",
        buttonText = "Begin"
    ),
    OnboardingPage(
        title = "Private by Design",
        description = "Monk has no internet access.\nNo servers. No accounts. No tracking.\nYour data stays on your device.",
        buttonText = "Continue",
        isPrivacyPage = true,
        privacyFeatures = listOf(
            "We don't know who you are",
            "Messages never leave your phone",
            "No sign-up, no account, no tracking",
            "Works completely offline"
        )
    ),
    OnboardingPage(
        title = "Notification Access",
        description = "Monk reads notifications to detect messages and send your auto-reply.\n\nTap below, find Monk, and turn it on.",
        buttonText = "Open Settings",
        permissionType = PermissionType.NOTIFICATION_LISTENER,
        isRequired = true
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    var notificationListenerEnabled by remember { mutableStateOf(false) }
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var runtimePermissionsGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        runtimePermissionsGranted = permissions.values.all { it }
        if (runtimePermissionsGranted) {
            scope.launch {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
                accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        notificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
        accessibilityEnabled = PermissionHelper.isAccessibilityServiceEnabled(context)
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
                .padding(horizontal = 32.dp)
        ) {
            // Top bar with skip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onComplete) {
                    Text(
                        "Skip",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }
            }

            // Pager - this is the main content area
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val currentPage = onboardingPages[page]
                val isPermissionGranted = when (currentPage.permissionType) {
                    PermissionType.NOTIFICATION_LISTENER -> notificationListenerEnabled
                    PermissionType.ACCESSIBILITY -> accessibilityEnabled
                    PermissionType.RUNTIME_PERMISSIONS -> runtimePermissionsGranted
                    PermissionType.NONE -> true
                }

                OnboardingPageContent(
                    page = currentPage,
                    isPermissionGranted = isPermissionGranted
                )
            }

            // Bottom section - indicators and button
            Column(
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                // Page indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(onboardingPages.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(
                                    width = if (index == pagerState.currentPage) 24.dp else 8.dp,
                                    height = 8.dp
                                )
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (index == pagerState.currentPage) Primary else Gray300
                                )
                        )
                    }
                }

                val currentPage = onboardingPages[pagerState.currentPage]
                val isCurrentPermissionGranted = when (currentPage.permissionType) {
                    PermissionType.NOTIFICATION_LISTENER -> notificationListenerEnabled
                    PermissionType.ACCESSIBILITY -> accessibilityEnabled
                    PermissionType.RUNTIME_PERMISSIONS -> runtimePermissionsGranted
                    PermissionType.NONE -> true
                }

                // Main action button
                Button(
                    onClick = {
                        when {
                            // Last page with permission granted -> done
                            pagerState.currentPage == onboardingPages.lastIndex && isCurrentPermissionGranted -> {
                                onComplete()
                            }
                            // Last page without permission -> open settings
                            currentPage.permissionType == PermissionType.NOTIFICATION_LISTENER && !isCurrentPermissionGranted -> {
                                PermissionHelper.openNotificationListenerSettings(context)
                            }
                            // Any other page -> next
                            else -> {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentPermissionGranted && currentPage.permissionType != PermissionType.NONE) 
                            Success else Primary
                    )
                ) {
                    Text(
                        text = when {
                            pagerState.currentPage == onboardingPages.lastIndex && isCurrentPermissionGranted -> "Start"
                            else -> currentPage.buttonText
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isPermissionGranted: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title - consistent across all pages
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            color = Primary,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description - consistent styling
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = TextMuted,
            lineHeight = 26.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        // Privacy page has feature list
        if (page.isPrivacyPage && page.privacyFeatures.isNotEmpty()) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 20.dp, horizontal = 24.dp)
                ) {
                    page.privacyFeatures.forEachIndexed { index, feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Primary)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = feature,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Primary
                            )
                        }
                        if (index < page.privacyFeatures.lastIndex) {
                            Divider(color = Divider)
                        }
                    }
                }
            }
        }

        // Permission status - only show when granted (as confirmation)
        if (page.permissionType != PermissionType.NONE && isPermissionGranted) {
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "✓ Enabled",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Success
            )
        }
    }
}
