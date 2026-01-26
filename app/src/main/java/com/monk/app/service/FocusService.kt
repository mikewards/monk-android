package com.monk.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.monk.app.MainActivity
import com.monk.app.MonkApp
import com.monk.app.R
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that keeps Monk running while focus mode is active.
 * This ensures the notification listener and auto-reply services stay active.
 */
@AndroidEntryPoint
class FocusService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startFocusMode()
            ACTION_STOP -> stopFocusMode()
        }
        return START_STICKY
    }

    private fun startFocusMode() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Notify other components that focus mode is active
        FocusManager.setActive(true)
    }

    private fun stopFocusMode() {
        FocusManager.setActive(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpenIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FocusService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStopIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MonkApp.FOCUS_CHANNEL_ID)
            .setContentTitle(getString(R.string.focus_notification_title))
            .setContentText(getString(R.string.focus_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setOngoing(true)
            .setContentIntent(pendingOpenIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Focus",
                pendingStopIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.monk.app.action.START_FOCUS"
        const val ACTION_STOP = "com.monk.app.action.STOP_FOCUS"

        fun start(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}

/**
 * Simple singleton to track focus state across services
 */
object FocusManager {
    @Volatile
    private var isActive: Boolean = false

    private val listeners = mutableListOf<(Boolean) -> Unit>()

    fun isActive(): Boolean = isActive

    fun setActive(active: Boolean) {
        isActive = active
        listeners.forEach { it(active) }
    }

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }
}
