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
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║                      PERMISSION HELPER                                  ║
 * ╠═══════════════════════════════════════════════════════════════════════╣
 * ║  Monk operates with MINIMAL permissions:                               ║
 * ║  • NotificationListener - Required for auto-reply                      ║
 * ║  • AccessibilityService - Optional enhanced compatibility              ║
 * ║  • READ_CONTACTS - Optional for whitelist feature                      ║
 * ║                                                                         ║
 * ║  NO INTERNET permission = Your data NEVER leaves your device          ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
object PermissionHelper {

    // ═══════════════════════════════════════════════════════════════
    // NOTIFICATION LISTENER PERMISSION (Required)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if Notification Listener permission is granted
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(packageName) == true
    }

    /**
     * Open Notification Listener settings
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ═══════════════════════════════════════════════════════════════
    // ACCESSIBILITY SERVICE PERMISSION (Optional)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if Accessibility Service is enabled
     * Uses multiple methods for reliability
     */
    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        // Method 1: Check if our service reports itself as connected (most reliable if service is running)
        if (AutoReplyService.isServiceConnected) {
            return true
        }
        
        // Method 2: Check Settings.Secure
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        
        // Check for our service in various formats
        val packageName = context.packageName
        val serviceName = AutoReplyService::class.java.canonicalName ?: AutoReplyService::class.java.name
        val serviceId = "$packageName/$serviceName"
        val shortServiceId = "$packageName/.service.AutoReplyService"
        
        if (enabledServices.contains(serviceId) || 
            enabledServices.contains(shortServiceId) ||
            enabledServices.contains("$packageName/com.monk.app.service.AutoReplyService")) {
            return true
        }
        
        // Method 3: Fallback to AccessibilityManager check
        try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServicesList = accessibilityManager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK
            )
            
            return enabledServicesList.any { serviceInfo ->
                serviceInfo.resolveInfo?.serviceInfo?.let { info ->
                    info.packageName == packageName
                } ?: false
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Open Accessibility settings
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ═══════════════════════════════════════════════════════════════
    // RUNTIME PERMISSIONS (Contacts, Notifications)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if Contacts permission is granted (for whitelist feature)
     */
    fun hasContactsPermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_CONTACTS)
    }

    /**
     * Check if POST_NOTIFICATIONS permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Not required before Android 13
        }
    }

    /**
     * Get list of all runtime permissions to request
     */
    fun getAllRuntimePermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        return permissions.toTypedArray()
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED
    }

    // ═══════════════════════════════════════════════════════════════
    // DO NOT DISTURB PERMISSION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if Do Not Disturb access is granted
     */
    fun isDndAccessGranted(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Open Do Not Disturb access settings
     */
    fun openDndAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Enable Do Not Disturb mode (total silence)
     */
    fun enableDnd(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            true
        } else {
            false
        }
    }

    /**
     * Disable Do Not Disturb mode (back to normal)
     */
    fun disableDnd(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            true
        } else {
            false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // BATTERY OPTIMIZATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Check if battery optimization is ignored (unrestricted)
     */
    fun isBatteryOptimizationIgnored(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Open battery optimization settings
     */
    fun openBatteryOptimizationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open Android's app notification settings (all apps)
     * Users can enable/disable notifications per app here
     */
    fun openAppNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // ═══════════════════════════════════════════════════════════════
    // PERMISSION STATUS SUMMARY
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get overall permission status
     */
    fun getPermissionStatus(context: Context): PermissionStatus {
        return PermissionStatus(
            notificationListener = isNotificationListenerEnabled(context),
            accessibilityService = isAccessibilityServiceEnabled(context),
            contactsPermission = hasContactsPermission(context),
            notificationPermission = hasNotificationPermission(context),
            dndAccess = isDndAccessGranted(context)
        )
    }

    /**
     * Check if all required permissions are granted
     * Note: Only NotificationListener is truly required.
     * AccessibilityService is optional (for apps without RemoteInput support)
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return isNotificationListenerEnabled(context)
    }
}

/**
 * Data class representing the current permission status
 */
data class PermissionStatus(
    val notificationListener: Boolean,
    val accessibilityService: Boolean,
    val contactsPermission: Boolean,
    val notificationPermission: Boolean,
    val dndAccess: Boolean = false
) {
    /**
     * Only NotificationListener is truly required.
     * Auto-reply uses RemoteInput through notifications.
     */
    val requiredPermissionsGranted: Boolean
        get() = notificationListener
    
    /**
     * AccessibilityService is optional but enhances compatibility
     */
    val optionalPermissionsGranted: Boolean
        get() = accessibilityService
}
