package com.monk.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.monk.app.service.AutoReplyManager
import com.monk.app.service.FocusManager

/**
 * Receiver for incoming SMS messages.
 * This provides native SMS auto-reply capability without needing accessibility services.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MonkSmsReceiver"
        private const val DEFAULT_REPLY = "Hey! I'm currently in focus mode and can't respond right now. I'll get back to you soon! 🧘"
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Only process if focus mode is active
        if (!FocusManager.isActive()) return

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        
        messages.forEach { sms ->
            val sender = sms.displayOriginatingAddress
            val messageBody = sms.messageBody

            Log.d(TAG, "SMS from $sender: $messageBody")

            // Check cooldown
            if (AutoReplyManager.isOnCooldown("sms", sender)) {
                Log.d(TAG, "On cooldown for $sender")
                return@forEach
            }

            // Send auto-reply via SMS
            try {
                sendSmsReply(context, sender)
                Log.d(TAG, "Auto-reply SMS sent to $sender")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS reply", e)
            }
        }
    }

    private fun sendSmsReply(context: Context, recipient: String) {
        try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            val replyMessage = getReplyMessage()
            
            // Split message if too long
            val parts = smsManager.divideMessage(replyMessage)
            
            if (parts.size == 1) {
                smsManager.sendTextMessage(recipient, null, replyMessage, null, null)
            } else {
                smsManager.sendMultipartTextMessage(recipient, null, parts, null, null)
            }

            // Record that we replied
            AutoReplyManager.queueReply(
                packageName = "sms",
                sender = recipient,
                originalMessage = ""
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
            throw e
        }
    }

    private fun getReplyMessage(): String {
        // TODO: Load from preferences
        return DEFAULT_REPLY
    }
}
