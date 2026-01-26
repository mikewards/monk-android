package com.monk.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.monk.app.domain.model.SupportedApp

/**
 * Accessibility service that sends auto-replies by interacting with messaging app UIs.
 * 
 * This service:
 * 1. Detects when a messaging app's chat window is open
 * 2. Finds the text input field and send button
 * 3. Types and sends the auto-reply message
 */
class AutoReplyService : AccessibilityService() {

    companion object {
        private const val TAG = "MonkAutoReplyService"
        
        // Default reply message (should be loaded from preferences)
        private const val DEFAULT_REPLY = "Hey! I'm currently in focus mode and can't respond right now. I'll get back to you soon! 🧘"
        
        @Volatile
        var instance: AutoReplyService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        
        Log.d(TAG, "AutoReplyService connected")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "AutoReplyService destroyed")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Only process if focus mode is active
        if (!FocusManager.isActive()) return

        val packageName = event.packageName?.toString() ?: return
        
        // Only process supported apps
        val app = SupportedApp.fromPackageName(packageName) ?: return

        // Check if we have a pending reply for this app
        if (!AutoReplyManager.hasPendingReply(packageName)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Try to send the reply
                trySendReply(app)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "AutoReplyService interrupted")
    }

    private fun trySendReply(app: SupportedApp) {
        val rootNode = rootInActiveWindow ?: return
        
        try {
            // Find the text input field
            val inputField = findInputField(rootNode, app)
            if (inputField == null) {
                Log.d(TAG, "Input field not found for ${app.displayName}")
                return
            }

            // Find the send button
            val sendButton = findSendButton(rootNode, app)
            if (sendButton == null) {
                Log.d(TAG, "Send button not found for ${app.displayName}")
                return
            }

            // Get the reply message
            val replyMessage = getReplyMessage()

            // Set the text in the input field
            val textArgs = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    replyMessage
                )
            }
            val textSet = inputField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, textArgs)
            
            if (!textSet) {
                Log.e(TAG, "Failed to set text in input field")
                return
            }

            // Small delay to ensure text is set
            Thread.sleep(100)

            // Click the send button
            val sent = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            if (sent) {
                // Mark the reply as sent
                AutoReplyManager.getPendingReply(app.packageName)?.let {
                    AutoReplyManager.markReplySent(it)
                }
                Log.d(TAG, "Successfully sent reply in ${app.displayName}")
            } else {
                Log.e(TAG, "Failed to click send button")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending reply", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun findInputField(root: AccessibilityNodeInfo, app: SupportedApp): AccessibilityNodeInfo? {
        // Try specific resource ID first
        app.inputFieldId?.let { id ->
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                return nodes[0]
            }
        }

        // Fallback: Find editable text field
        return findNodeByPredicate(root) { node ->
            node.isEditable && node.className?.contains("EditText") == true
        }
    }

    private fun findSendButton(root: AccessibilityNodeInfo, app: SupportedApp): AccessibilityNodeInfo? {
        // Try specific resource ID first
        app.sendButtonId?.let { id ->
            val nodes = root.findAccessibilityNodeInfosByViewId(id)
            if (nodes.isNotEmpty()) {
                return nodes[0]
            }
        }

        // Try content description
        app.sendButtonContentDesc?.let { desc ->
            return findNodeByPredicate(root) { node ->
                node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true &&
                        node.isClickable
            }
        }

        // Fallback: Find clickable node with "send" in content description
        return findNodeByPredicate(root) { node ->
            (node.contentDescription?.toString()?.contains("send", ignoreCase = true) == true ||
                    node.text?.toString()?.contains("send", ignoreCase = true) == true) &&
                    node.isClickable
        }
    }

    private fun findNodeByPredicate(
        root: AccessibilityNodeInfo,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): AccessibilityNodeInfo? {
        if (predicate(root)) return root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findNodeByPredicate(child, predicate)
            if (result != null) return result
            child.recycle()
        }

        return null
    }

    private fun getReplyMessage(): String {
        // TODO: Load from preferences
        return DEFAULT_REPLY
    }
}
