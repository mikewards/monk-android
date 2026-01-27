package com.monk.app.util

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.monk.app.service.AutoReplyService

/**
 * Centralized permission checking and settings navigation.
 *
 * Monk requires minimal permissions:
 * - NotificationListener: Required for auto-reply
 * - AccessibilityService: Optional, for deep focus mode
 * - DND Access: Optional, for silencing phone
 * - Contacts: Optional, for whitelist feature
 *
 * No internet permission - data never leaves the device.
 */
object PermissionHelper {

    // Notification Listener (required)

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(context.packageName) == true
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Accessibility Service (optional)

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        if (AutoReplyService.isServiceConnected) return true

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        val packageName = context.packageName
        val servicePatterns = listOf(
            "$packageName/${AutoReplyService::class.java.canonicalName}",
            "$packageName/.service.AutoReplyService",
            "$packageName/com.monk.app.service.AutoReplyService"
        )

        if (servicePatterns.any { enabledServices.contains(it) }) return true

        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo?.serviceInfo?.packageName == packageName }
        } catch (e: Exception) {
            false
        }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Do Not Disturb (optional)

    fun isDndAccessGranted(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.isNotificationPolicyAccessGranted
    }

    fun openDndAccessSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun enableDnd(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            true
        } else false
    }

    fun disableDnd(context: Context): Boolean {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (nm.isNotificationPolicyAccessGranted) {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            true
        } else false
    }

    // Contacts (optional)

    fun hasContactsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    // Notifications (Android 13+)

    fun hasNotificationPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else true

    // Battery optimization

    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatteryOptimizationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // App notification settings

    fun openAppNotificationSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Aggregate status

    fun getPermissionStatus(context: Context) = PermissionStatus(
        notificationListener = isNotificationListenerEnabled(context),
        accessibilityService = isAccessibilityServiceEnabled(context),
        contactsPermission = hasContactsPermission(context),
        notificationPermission = hasNotificationPermission(context),
        dndAccess = isDndAccessGranted(context)
    )

    fun hasAllRequiredPermissions(context: Context): Boolean =
        isNotificationListenerEnabled(context)
}

data class PermissionStatus(
    val notificationListener: Boolean,
    val accessibilityService: Boolean,
    val contactsPermission: Boolean,
    val notificationPermission: Boolean,
    val dndAccess: Boolean = false
) {
    val requiredPermissionsGranted: Boolean get() = notificationListener
}
