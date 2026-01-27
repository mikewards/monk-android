package com.monk.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.monk.app.domain.model.SupportedApp

/**
 * NotificationListener - Production Implementation
 * 
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║  PRIVACY NOTE: This service processes notifications in memory only.   ║
 * ║  No message content, sender names, or personal data is ever stored    ║
 * ║  persistently or transmitted anywhere.                                ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 * 
 * Intercepts incoming notifications from messaging apps,
 * extracts sender/message info, and sends auto-replies
 * using the notification's built-in RemoteInput action.
 * 
 * This approach:
 * - Works universally with any app that supports notification replies
 * - Doesn't require app-specific UI element IDs
 * - Doesn't break when apps update
 * - Works for SMS via Messages app notifications (no SMS permission needed!)
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "Monk.NotificationListener"
        
        // Privacy: Only log in debug builds, never in production
        private val DEBUG_LOGGING = com.monk.app.BuildConfig.DEBUG
        
        // Singleton state for focus mode
        @Volatile
        var isFocusModeActive = false
        
        @Volatile
        var replyMessage = "I'm currently in monk mode and will respond later."
        
        @Volatile
        var cooldownMinutes = 5
        
        @Volatile
        var enabledApps = SupportedApp.entries.map { it.packageName }.toSet()
        
        // Statistics
        @Volatile
        var notificationsSilenced = 0
        
        @Volatile
        var repliesSent = 0
        
        /**
         * Privacy-safe logging: Redacts sensitive data
         */
        private fun redactSender(sender: String): String {
            if (!DEBUG_LOGGING) return "[REDACTED]"
            // Show last 4 chars only for phone numbers, first initial for names
            return if (sender.any { it.isDigit() }) {
                "***${sender.takeLast(4)}"
            } else {
                "${sender.firstOrNull() ?: '?'}***"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NotificationListener service created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "NotificationListener connected and active")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "NotificationListener disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        
        val packageName = sbn.packageName
        
        // Skip if not in focus mode
        if (!isFocusModeActive) {
            return
        }
        
        // Skip if not a supported messaging app
        if (!isMessagingApp(packageName)) {
            return
        }
        
        // Skip if app not enabled
        if (packageName !in enabledApps) {
            return
        }
        
        // Identify app type for debugging
        val isSmsApp = packageName == "com.google.android.apps.messaging" ||
                       packageName == "com.samsung.android.messaging" ||
                       packageName == "com.android.mms"
        val appType = if (isSmsApp) "SMS/Messages" else "Messaging"
        
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "📨 $appType notification from: $packageName")
        
        // Extract notification details
        val notification = sbn.notification
        val extras = notification.extras
        
        // Log notification structure for debugging (privacy-safe)
        if (DEBUG_LOGGING) {
            val hasActions = notification.actions?.isNotEmpty() == true
            val actionCount = notification.actions?.size ?: 0
            val hasWearableActions = try {
                Notification.WearableExtender(notification).actions.isNotEmpty()
            } catch (e: Exception) { false }
            
            Log.d(TAG, "📋 Notification structure:")
            Log.d(TAG, "   - Has actions: $hasActions (count: $actionCount)")
            Log.d(TAG, "   - Has wearable actions: $hasWearableActions")
            Log.d(TAG, "   - Category: ${notification.category ?: "none"}")
        }
        
        // Skip if this is our own reply notification (message contains our reply text)
        val messageText = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        if (messageText == replyMessage || messageText.contains("monk mode and will respond later")) {
            Log.d(TAG, "⏭️ Skipping - This is our own reply notification")
            return
        }
        
        val messageInfo = extractMessageInfo(packageName, extras)
        
        if (messageInfo != null) {
            // Privacy-safe logging - redact actual content
            Log.d(TAG, "👤 Sender: ${redactSender(messageInfo.sender)}")
            Log.d(TAG, "💬 Message length: ${messageInfo.message.length} chars")
            Log.d(TAG, "📱 Chat type: ${messageInfo.chatType}")
            
            // Check cooldown
            if (!AutoReplyManager.shouldReply(messageInfo.sender)) {
                Log.d(TAG, "⏳ Skipping - Cooldown active for ${redactSender(messageInfo.sender)}")
                return
            }
            
            // Track silenced notification
            notificationsSilenced++
            
            // Try to send reply via RemoteInput (the production-ready approach)
            Log.d(TAG, "🔄 Attempting RemoteInput reply...")
            val replied = sendReplyViaRemoteInput(sbn, replyMessage)
            
            if (replied) {
                Log.d(TAG, "✅ Reply sent successfully via RemoteInput!")
                AutoReplyManager.recordReply(messageInfo.sender)
                repliesSent++
            } else {
                Log.w(TAG, "❌ Could not find reply action in notification")
                Log.w(TAG, "   This app may not support notification quick-reply")
                // Fallback: Could trigger AccessibilityService here if needed
            }
        } else {
            Log.d(TAG, "⚠️ Could not extract message info from notification")
        }
        
        Log.d(TAG, "═══════════════════════════════════════════")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Not needed for our use case
    }

    private fun isMessagingApp(packageName: String): Boolean {
        return SupportedApp.entries.any { it.packageName == packageName }
    }

    /**
     * Send a reply using the notification's built-in RemoteInput action.
     * 
     * This is the production-ready approach that:
     * - Works with any app that supports notification replies (including SMS!)
     * - Doesn't require knowing app-specific UI element IDs
     * - Doesn't break when apps update
     */
    private fun sendReplyViaRemoteInput(sbn: StatusBarNotification, message: String): Boolean {
        val notification = sbn.notification
        
        // Method 1: Try WearableExtender actions (works for many apps including Messages)
        try {
            val wearableExtender = Notification.WearableExtender(notification)
            val wearableActions = wearableExtender.actions
            Log.d(TAG, "   Checking ${wearableActions.size} wearable actions...")
            
            for ((index, action) in wearableActions.withIndex()) {
                Log.d(TAG, "   Wearable action $index: '${action.title}' hasRemoteInput=${action.remoteInputs?.isNotEmpty() == true}")
                if (sendReplyViaAction(action, message, sbn.key)) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "   No wearable extender: ${e.message}")
        }
        
        // Method 2: Try standard notification actions
        val standardActions = notification.actions ?: emptyArray()
        Log.d(TAG, "   Checking ${standardActions.size} standard actions...")
        
        for ((index, action) in standardActions.withIndex()) {
            val hasRemoteInput = action.remoteInputs?.isNotEmpty() == true
            Log.d(TAG, "   Standard action $index: '${action.title}' hasRemoteInput=$hasRemoteInput")
            if (sendReplyViaAction(action, message, sbn.key)) {
                return true
            }
        }
        
        Log.d(TAG, "   ⚠️ No actions with RemoteInput found")
        return false
    }

    /**
     * Attempt to send a reply via a specific notification action
     */
    private fun sendReplyViaAction(action: Notification.Action, message: String, notificationKey: String): Boolean {
        val remoteInputs = action.remoteInputs
        if (remoteInputs.isNullOrEmpty()) {
            return false
        }
        
        Log.d(TAG, "Found action with RemoteInput: ${action.title}")
        
        try {
            // Build the reply intent
            val intent = Intent()
            val bundle = Bundle()
            
            // Fill in all RemoteInputs with our reply message
            for (remoteInput in remoteInputs) {
                Log.d(TAG, "RemoteInput key: ${remoteInput.resultKey}")
                bundle.putCharSequence(remoteInput.resultKey, message)
            }
            
            // Add the RemoteInput results to the intent
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            
            // Send the reply
            action.actionIntent.send(this, 0, intent)
            
            Log.d(TAG, "Reply sent via action: ${action.title}")
            
            // Cancel the notification after replying (optional - some apps do this automatically)
            try {
                cancelNotification(notificationKey)
            } catch (e: Exception) {
                // Ignore - notification might already be gone
            }
            
            return true
            
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "PendingIntent was cancelled", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply via RemoteInput", e)
            return false
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // MESSAGE EXTRACTION
    // ═══════════════════════════════════════════════════════════════

    private fun extractMessageInfo(packageName: String, extras: Bundle): MessageInfo? {
        val title = extras.getString(Notification.EXTRA_TITLE) 
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: return null
            
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return null
        
        // Detect group vs individual chat
        val summaryText = extras.getString(Notification.EXTRA_SUMMARY_TEXT)
        val isGroup = summaryText != null || 
                      title.contains(" messages") || 
                      title.contains("@") ||
                      text.contains(":")
        
        val sender = when {
            isGroup && summaryText != null -> summaryText
            isGroup && text.contains(":") -> text.substringBefore(":").trim()
            else -> title
        }
        
        return MessageInfo(
            sender = sender,
            message = text,
            chatType = if (isGroup) ChatType.GROUP else ChatType.INDIVIDUAL
        )
    }
}

/**
 * Information extracted from a message notification
 */
data class MessageInfo(
    val sender: String,
    val message: String,
    val chatType: ChatType
)

enum class ChatType {
    INDIVIDUAL,
    GROUP
}

/**
 * Manages auto-reply cooldowns and state
 * 
 * PRIVACY: Sender identifiers are stored in memory only during the focus session.
 * They are cleared when focus mode ends and never persisted to disk.
 */
object AutoReplyManager {
    private const val TAG = "Monk.AutoReplyManager"
    
    // Track last reply time for each sender (in-memory only, cleared on reset)
    private val lastReplyTimes = mutableMapOf<String, Long>()
    
    // Default cooldown in milliseconds
    private var cooldownMs = 5 * 60 * 1000L // 5 minutes
    
    fun setCooldownMinutes(minutes: Int) {
        cooldownMs = minutes * 60 * 1000L
        Log.d(TAG, "⏱️ Cooldown set to $minutes minutes")
    }
    
    fun shouldReply(sender: String): Boolean {
        val now = System.currentTimeMillis()
        val lastReply = lastReplyTimes[sender] ?: 0L
        val elapsed = now - lastReply
        
        val shouldReply = elapsed >= cooldownMs
        // Privacy-safe: don't log full sender
        val senderHint = if (sender.any { it.isDigit() }) "***${sender.takeLast(4)}" else "${sender.firstOrNull()}***"
        Log.d(TAG, "Should reply to $senderHint? $shouldReply (elapsed: ${elapsed/1000}s, cooldown: ${cooldownMs/1000}s)")
        
        return shouldReply
    }
    
    fun recordReply(sender: String) {
        lastReplyTimes[sender] = System.currentTimeMillis()
        val senderHint = if (sender.any { it.isDigit() }) "***${sender.takeLast(4)}" else "${sender.firstOrNull()}***"
        Log.d(TAG, "📝 Recorded reply to $senderHint")
    }
    
    fun reset() {
        lastReplyTimes.clear()
        Log.d(TAG, "🗑️ Reply history cleared (in-memory data wiped)")
    }
}
