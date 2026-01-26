package com.monk.app.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.monk.app.domain.model.SupportedApp

/**
 * Listens to all notifications and:
 * 1. Detects incoming messages from supported apps
 * 2. Silences notifications during focus mode
 * 3. Triggers auto-replies via AutoReplyManager
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "MonkNotificationListener"

        // Notification extras keys for extracting message data
        private const val EXTRA_TEXT = Notification.EXTRA_TEXT
        private const val EXTRA_TITLE = Notification.EXTRA_TITLE
        private const val EXTRA_BIG_TEXT = Notification.EXTRA_BIG_TEXT
        private const val EXTRA_MESSAGES = Notification.EXTRA_MESSAGES
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Skip if focus mode is not active
        if (!FocusManager.isActive()) return

        val packageName = sbn.packageName

        // Skip our own notifications
        if (packageName == "com.monk.app") return

        // Check if this is a supported messaging app
        val app = SupportedApp.fromPackageName(packageName) ?: return

        Log.d(TAG, "Notification from ${app.displayName}")

        // Extract message info
        val extras = sbn.notification.extras
        val messageInfo = extractMessageInfo(extras, packageName)

        if (messageInfo != null) {
            Log.d(TAG, "Message from ${messageInfo.sender}: ${messageInfo.text}")

            // Check if sender is whitelisted
            if (isWhitelisted(messageInfo.sender)) {
                Log.d(TAG, "Sender is whitelisted, not auto-replying")
                return
            }

            // Check cooldown (don't spam the same person)
            if (AutoReplyManager.isOnCooldown(packageName, messageInfo.sender)) {
                Log.d(TAG, "On cooldown for ${messageInfo.sender}")
                return
            }

            // Queue auto-reply
            AutoReplyManager.queueReply(
                packageName = packageName,
                sender = messageInfo.sender,
                originalMessage = messageInfo.text
            )

            // Optionally silence/dismiss the notification
            if (shouldSilenceNotification()) {
                try {
                    cancelNotification(sbn.key)
                    Log.d(TAG, "Silenced notification from ${app.displayName}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel notification", e)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Could track when user manually dismisses notifications
    }

    private fun extractMessageInfo(extras: Bundle, packageName: String): MessageInfo? {
        return try {
            val title = extras.getCharSequence(EXTRA_TITLE)?.toString()
            val text = extras.getCharSequence(EXTRA_TEXT)?.toString()
                ?: extras.getCharSequence(EXTRA_BIG_TEXT)?.toString()

            // For group messages, try to get the latest message
            val messages = extras.getParcelableArray(EXTRA_MESSAGES)
            val latestMessage = messages?.lastOrNull()

            // Different apps structure their notifications differently
            val sender = when {
                // WhatsApp includes sender in title
                packageName.startsWith("com.whatsapp") -> title
                // Messenger/Instagram might include sender differently
                else -> title
            }

            if (sender != null && text != null) {
                MessageInfo(sender = sender, text = text)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting message info", e)
            null
        }
    }

    private fun isWhitelisted(sender: String): Boolean {
        // TODO: Check against whitelist stored in preferences/database
        return false
    }

    private fun shouldSilenceNotification(): Boolean {
        // TODO: Check user preference
        return true
    }

    private data class MessageInfo(
        val sender: String,
        val text: String
    )
}

/**
 * Manages the auto-reply queue and cooldowns
 */
object AutoReplyManager {
    private const val DEFAULT_COOLDOWN_MS = 5 * 60 * 1000L // 5 minutes

    // Track when we last replied to each person in each app
    private val lastReplyTimes = mutableMapOf<String, Long>()

    // Pending replies to be processed by AccessibilityService
    private val pendingReplies = mutableListOf<PendingReply>()

    fun isOnCooldown(packageName: String, sender: String): Boolean {
        val key = "$packageName:$sender"
        val lastReply = lastReplyTimes[key] ?: return false
        return System.currentTimeMillis() - lastReply < DEFAULT_COOLDOWN_MS
    }

    fun queueReply(packageName: String, sender: String, originalMessage: String) {
        val key = "$packageName:$sender"
        lastReplyTimes[key] = System.currentTimeMillis()

        pendingReplies.add(
            PendingReply(
                packageName = packageName,
                sender = sender,
                originalMessage = originalMessage,
                timestamp = System.currentTimeMillis()
            )
        )

        Log.d("AutoReplyManager", "Queued reply for $sender in $packageName")
    }

    fun getPendingReply(packageName: String): PendingReply? {
        return pendingReplies.find { it.packageName == packageName }
    }

    fun markReplySent(reply: PendingReply) {
        pendingReplies.remove(reply)
        Log.d("AutoReplyManager", "Reply sent to ${reply.sender}")
    }

    fun hasPendingReply(packageName: String): Boolean {
        return pendingReplies.any { it.packageName == packageName }
    }

    data class PendingReply(
        val packageName: String,
        val sender: String,
        val originalMessage: String,
        val timestamp: Long
    )
}
