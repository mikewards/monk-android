package com.monk.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.monk.app.MainActivity
import com.monk.app.R
import com.monk.app.data.datastore.PreferencesManager
import com.monk.app.util.PermissionHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Foreground service managing focus mode lifecycle.
 * Coordinates NotificationListener and AutoReplyService.
 */
class FocusService : Service() {

    companion object {
        private const val TAG = "FocusService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "monk_focus_channel"

        private const val ACTION_START = "com.monk.app.START_FOCUS"
        private const val ACTION_STOP = "com.monk.app.STOP_FOCUS"
        private const val ACTION_RESUME = "com.monk.app.RESUME_FOCUS"
        private const val EXTRA_DURATION_MS = "duration_ms"

        @Volatile var isRunning = false
            private set

        @Volatile var focusStartTimeMs: Long = 0
            private set

        @Volatile var focusDurationMs: Long = 0
            private set

        @Volatile var deepFocusEnabled = false
        @Volatile var dndEnabled = false

        fun start(context: Context, durationMs: Long = 0) {
            Log.d(TAG, "Starting with duration: ${durationMs}ms")
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MS, durationMs)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            Log.d(TAG, "Stopping")
            context.startService(
                Intent(context, FocusService::class.java).apply { action = ACTION_STOP }
            )
        }

        fun resume(context: Context) {
            Log.d(TAG, "Resuming after boot")
            context.startService(
                Intent(context, FocusService::class.java).apply { action = ACTION_RESUME }
            )
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferencesManager: PreferencesManager
    private var updateJob: Job? = null
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        preferencesManager = PreferencesManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0)
                startFocusMode(durationMs)
            }
            ACTION_STOP -> stopFocusMode()
            ACTION_RESUME -> resumeFromBoot()
            else -> if (isRunning) resumeFocusMode()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.d(TAG, "Destroyed")
    }

    private fun startFocusMode(durationMs: Long) {
        if (isRunning) {
            Log.d(TAG, "Already running")
            return
        }

        isRunning = true
        focusStartTimeMs = System.currentTimeMillis()
        focusDurationMs = durationMs

        // Persist for boot resume
        serviceScope.launch {
            preferencesManager.setFocusSession(true, focusStartTimeMs, focusDurationMs)
        }

        NotificationListener.isFocusModeActive = true
        NotificationListener.notificationsSilenced = 0
        NotificationListener.repliesSent = 0
        AutoReplyManager.reset()
        AutoReplyService.deepFocusActive = deepFocusEnabled

        if (dndEnabled) {
            PermissionHelper.enableDnd(this)
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startNotificationUpdates()

        if (durationMs > 0) {
            startTimer(durationMs)
        }

        Log.d(TAG, "Focus started - duration: ${durationMs / 60000}min, deepFocus: $deepFocusEnabled, dnd: $dndEnabled")
    }

    private fun resumeFromBoot() {
        serviceScope.launch {
            val wasActive = preferencesManager.focusActive.first()
            if (!wasActive) {
                Log.d(TAG, "No active session to resume")
                stopSelf()
                return@launch
            }

            val startTime = preferencesManager.focusStartTime.first()
            val duration = preferencesManager.focusDurationMs.first()

            // Check if timed session has expired
            if (duration > 0) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= duration) {
                    Log.d(TAG, "Session expired during boot, clearing")
                    preferencesManager.setFocusSession(false)
                    stopSelf()
                    return@launch
                }
            }

            // Resume the session
            focusStartTimeMs = startTime
            focusDurationMs = duration
            isRunning = true

            // Load preferences
            deepFocusEnabled = preferencesManager.deepFocusEnabled.first()
            dndEnabled = preferencesManager.dndEnabled.first()

            NotificationListener.isFocusModeActive = true
            AutoReplyService.deepFocusActive = deepFocusEnabled

            if (dndEnabled) {
                PermissionHelper.enableDnd(this@FocusService)
            }

            startForeground(NOTIFICATION_ID, createNotification())
            startNotificationUpdates()

            if (duration > 0) {
                val remaining = duration - (System.currentTimeMillis() - startTime)
                startTimer(remaining)
            }

            Log.d(TAG, "Resumed from boot")
        }
    }

    private fun resumeFocusMode() {
        Log.d(TAG, "Resuming")

        if (focusDurationMs > 0) {
            val elapsed = System.currentTimeMillis() - focusStartTimeMs
            val remaining = focusDurationMs - elapsed

            if (remaining <= 0) {
                Log.d(TAG, "Timer expired, stopping")
                stopFocusMode()
                return
            }
            startTimer(remaining)
        }

        NotificationListener.isFocusModeActive = true
        AutoReplyService.deepFocusActive = deepFocusEnabled

        startForeground(NOTIFICATION_ID, createNotification())
        startNotificationUpdates()
    }

    private fun stopFocusMode() {
        if (!isRunning) return

        isRunning = false

        // Clear persisted state
        serviceScope.launch {
            preferencesManager.setFocusSession(false)
        }

        NotificationListener.isFocusModeActive = false
        AutoReplyService.deepFocusActive = false

        if (dndEnabled) {
            PermissionHelper.disableDnd(this)
        }

        updateJob?.cancel()
        timerJob?.cancel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        val durationMin = (System.currentTimeMillis() - focusStartTimeMs) / 60000
        Log.d(TAG, "Focus ended - ${durationMin}min, silenced: ${NotificationListener.notificationsSilenced}, replies: ${NotificationListener.repliesSent}")

        focusStartTimeMs = 0
        focusDurationMs = 0

        stopSelf()
    }

    private fun startTimer(durationMs: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            delay(durationMs)
            Log.d(TAG, "Timer expired")
            stopFocusMode()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Mode",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when focus mode is active"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stats = "${NotificationListener.notificationsSilenced} silenced, ${NotificationListener.repliesSent} replies"

        val timeText = if (focusDurationMs > 0) {
            val remaining = focusDurationMs - (System.currentTimeMillis() - focusStartTimeMs)
            "${(remaining / 60000).coerceAtLeast(0)}m left"
        } else {
            "${(System.currentTimeMillis() - focusStartTimeMs) / 60000}m"
        }

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, FocusService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus mode active")
            .setContentText(stats)
            .setSubText(timeText)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openIntent)
            .addAction(0, "End Session", stopIntent)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startNotificationUpdates() {
        updateJob?.cancel()
        updateJob = serviceScope.launch {
            while (isActive && isRunning) {
                delay(60_000)
                if (isRunning) {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }
}
