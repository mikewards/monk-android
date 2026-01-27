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
 * Intercepts incoming notifications from messaging apps and sends auto-replies
 * using the notification's built-in RemoteInput action.
 *
 * Privacy: Processes notifications in memory only. No message content, sender names,
 * or personal data is ever stored persistently or transmitted.
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"
        private val DEBUG = com.monk.app.BuildConfig.DEBUG

        @Volatile var isFocusModeActive = false
        @Volatile var replyMessage = "I'm currently in monk mode and will respond later."
        @Volatile var cooldownMinutes = 5
        @Volatile var enabledApps = SupportedApp.entries.map { it.packageName }.toSet()
        @Volatile var notificationsSilenced = 0
        @Volatile var repliesSent = 0

        private fun redactSender(sender: String): String {
            if (!DEBUG) return "[REDACTED]"
            return if (sender.any { it.isDigit() }) {
                "***${sender.takeLast(4)}"
            } else {
                "${sender.firstOrNull() ?: '?'}***"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName

        if (!isFocusModeActive) return
        if (!isMessagingApp(packageName)) return
        if (packageName !in enabledApps) return

        val notification = sbn.notification
        val extras = notification.extras

        Log.d(TAG, "Notification from $packageName")

        // Skip our own reply notifications
        val messageText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (messageText == replyMessage || messageText.contains("monk mode")) {
            Log.d(TAG, "Skipping own reply notification")
            return
        }

        val messageInfo = extractMessageInfo(packageName, extras)

        if (messageInfo != null) {
            Log.d(TAG, "Sender: ${redactSender(messageInfo.sender)}, length: ${messageInfo.message.length}")

            if (!AutoReplyManager.shouldReply(messageInfo.sender)) {
                Log.d(TAG, "Cooldown active for ${redactSender(messageInfo.sender)}")
                return
            }

            notificationsSilenced++

            val replied = sendReplyViaRemoteInput(sbn, replyMessage)

            if (replied) {
                Log.d(TAG, "Reply sent")
                AutoReplyManager.recordReply(messageInfo.sender)
                repliesSent++
            } else {
                Log.w(TAG, "No reply action found")
            }
        } else {
            Log.d(TAG, "Could not extract message info")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    private fun isMessagingApp(packageName: String): Boolean {
        return SupportedApp.entries.any { it.packageName == packageName }
    }

    private fun sendReplyViaRemoteInput(sbn: StatusBarNotification, message: String): Boolean {
        val notification = sbn.notification

        // Try WearableExtender actions first
        try {
            val wearableExtender = Notification.WearableExtender(notification)
            for (action in wearableExtender.actions) {
                if (sendReplyViaAction(action, message, sbn.key)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // No wearable extender
        }

        // Try standard notification actions
        val standardActions = notification.actions ?: emptyArray()
        for (action in standardActions) {
            if (sendReplyViaAction(action, message, sbn.key)) {
                return true
            }
        }

        return false
    }

    private fun sendReplyViaAction(action: Notification.Action, message: String, notificationKey: String): Boolean {
        val remoteInputs = action.remoteInputs
        if (remoteInputs.isNullOrEmpty()) return false

        try {
            val intent = Intent()
            val bundle = Bundle()

            for (remoteInput in remoteInputs) {
                bundle.putCharSequence(remoteInput.resultKey, message)
            }

            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            action.actionIntent.send(this, 0, intent)

            try {
                cancelNotification(notificationKey)
            } catch (e: Exception) {
                // Notification might already be gone
            }

            return true
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "PendingIntent cancelled", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply", e)
            return false
        }
    }

    private fun extractMessageInfo(packageName: String, extras: Bundle): MessageInfo? {
        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: return null

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return null

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
 * Manages auto-reply cooldowns.
 *
 * Privacy: Sender identifiers stored in memory only during focus session.
 * Cleared when focus mode ends, never persisted to disk.
 */
object AutoReplyManager {
    private const val TAG = "AutoReplyManager"

    private val lastReplyTimes = mutableMapOf<String, Long>()
    private var cooldownMs = 5 * 60 * 1000L

    fun setCooldownMinutes(minutes: Int) {
        cooldownMs = minutes * 60 * 1000L
        Log.d(TAG, "Cooldown set to $minutes minutes")
    }

    fun shouldReply(sender: String): Boolean {
        val now = System.currentTimeMillis()
        val lastReply = lastReplyTimes[sender] ?: 0L
        return (now - lastReply) >= cooldownMs
    }

    fun recordReply(sender: String) {
        lastReplyTimes[sender] = System.currentTimeMillis()
    }

    fun reset() {
        lastReplyTimes.clear()
        Log.d(TAG, "Reply history cleared")
    }
}
