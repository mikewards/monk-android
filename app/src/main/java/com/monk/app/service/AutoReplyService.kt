package com.monk.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.monk.app.MainActivity

/**
 * AutoReplyService - Accessibility Service with two purposes:
 * 
 * 1. FALLBACK AUTO-REPLY: For apps that don't support notification quick-reply
 * 2. DEEP FOCUS: Brings user back to Monk when they try to leave during focus
 */
class AutoReplyService : AccessibilityService() {

    companion object {
        private const val TAG = "Monk.AutoReplyService"
        
        const val ACTION_SEND_REPLY = "com.monk.app.ACTION_SEND_REPLY"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_REPLY_MESSAGE = "reply_message"
        
        private const val MONK_PACKAGE = "com.monk.app"
        
        @Volatile
        var isServiceConnected = false
        
        /**
         * When true, the service will bring user back to Monk
         * whenever they try to switch to another app
         */
        @Volatile
        var deepFocusActive = false
    }

    private var pendingReply: PendingReply? = null
    private var lastReopenTime = 0L
    private val reopenCooldownMs = 500L // Prevent rapid reopening
    
    private val replyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_SEND_REPLY) {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: return
                val replyMessage = intent.getStringExtra(EXTRA_REPLY_MESSAGE) ?: return
                
                Log.d(TAG, "Received fallback reply request for $packageName to $sender")
                
                pendingReply = PendingReply(packageName, sender, replyMessage)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "AutoReplyService created")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
        Log.d(TAG, "AutoReplyService connected")
        
        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        // Register for reply broadcasts
        val filter = IntentFilter(ACTION_SEND_REPLY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(replyReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(replyReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        deepFocusActive = false
        try {
            unregisterReceiver(replyReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
        Log.d(TAG, "AutoReplyService destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        val packageName = event.packageName?.toString() ?: return
        
        // Deep Focus: Bring user back to Monk if they try to leave
        if (deepFocusActive && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleDeepFocus(packageName)
        }
        
        // Fallback reply handling
        val pending = pendingReply
        if (pending != null && pending.packageName == packageName) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    tryGenericReply(pending)
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoReplyService interrupted")
    }

    /**
     * Deep Focus: Detect when user leaves Monk and bring them back
     */
    private fun handleDeepFocus(currentPackage: String) {
        // Only ignore system UI (notification shade, settings, etc.) - NOT launchers
        val systemPackages = listOf(
            "com.android.systemui",
            MONK_PACKAGE
        )
        
        if (systemPackages.any { currentPackage.contains(it) }) {
            return
        }
        
        // User switched to another app or home screen - bring them back!
        val now = System.currentTimeMillis()
        if (now - lastReopenTime < reopenCooldownMs) {
            return // Cooldown to prevent rapid reopening
        }
        
        Log.d(TAG, "Deep Focus: User tried to open $currentPackage - bringing back to Monk")
        lastReopenTime = now
        
        // Launch Monk
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    /**
     * Try to send a reply using generic UI element detection.
     * This is a best-effort fallback that may not work with all apps.
     */
    private fun tryGenericReply(pending: PendingReply) {
        val rootNode = rootInActiveWindow ?: return
        
        // Find any editable text field
        val inputField = findEditableField(rootNode)
        if (inputField == null) {
            Log.d(TAG, "No editable field found")
            return
        }
        
        // Try to set text
        val textSet = setTextInField(inputField, pending.message)
        if (!textSet) {
            Log.d(TAG, "Failed to set text")
            return
        }
        
        // Find and click send button
        val sendButton = findSendButton(rootNode)
        if (sendButton == null) {
            Log.d(TAG, "No send button found")
            return
        }
        
        if (sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            Log.d(TAG, "Fallback reply sent via AccessibilityService")
            NotificationListener.repliesSent++
            pendingReply = null
        }
    }

    private fun findEditableField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for editable text fields
        if (node.isEditable && node.className?.contains("EditText") == true) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findEditableField(child)
            if (result != null) return result
        }
        
        return null
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Look for send buttons by content description or text
        val sendKeywords = listOf("send", "submit", "post", "reply")
        
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        
        if (node.isClickable) {
            for (keyword in sendKeywords) {
                if (contentDesc.contains(keyword) || text.contains(keyword) || viewId.contains(keyword)) {
                    return node
                }
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findSendButton(child)
            if (result != null) return result
        }
        
        return null
    }

    private fun setTextInField(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }
}

/**
 * Represents a reply that's waiting to be sent
 */
data class PendingReply(
    val packageName: String,
    val sender: String,
    val message: String
)
