package com.monk.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver that starts the app after device boot if focus mode was active
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MonkBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, checking if focus mode should resume")
            
            // TODO: Check preferences if focus mode should auto-start
            // For now, we don't auto-start focus mode after boot
        }
    }
}
