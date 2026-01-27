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
import com.monk.app.util.PermissionHelper
import kotlinx.coroutines.*

/**
 * FocusService - Foreground service that manages focus mode lifecycle.
 * Coordinates between NotificationListener and AutoReplyService.
 */
class FocusService : Service() {

    companion object {
        private const val TAG = "Monk.FocusService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "monk_focus_channel"
        
        private const val ACTION_START = "com.monk.app.START_FOCUS"
        private const val ACTION_STOP = "com.monk.app.STOP_FOCUS"
        private const val EXTRA_DURATION_MS = "duration_ms"
        
        @Volatile
        var isRunning = false
            private set
        
        /**
         * When the focus session started (persisted across app restarts)
         */
        @Volatile
        var focusStartTimeMs: Long = 0
            private set
        
        /**
         * Duration of the focus session in milliseconds (0 = indefinite)
         */
        @Volatile
        var focusDurationMs: Long = 0
            private set
        
        /**
         * Flag for Deep Focus mode (auto-reopen when user leaves)
         */
        @Volatile
        var deepFocusEnabled = false
        
        /**
         * Flag for Do Not Disturb mode (silence phone during focus)
         */
        @Volatile
        var dndEnabled = false
        
        fun start(context: Context, durationMs: Long = 0) {
            Log.d(TAG, "Starting FocusService with duration: ${durationMs}ms")
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
            Log.d(TAG, "Stopping FocusService")
            val intent = Intent(context, FocusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var updateJob: Job? = null
    private var timerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FocusService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0)
                startFocusMode(durationMs)
            }
            ACTION_STOP -> stopFocusMode()
            else -> {
                // Service restarted by system, resume focus mode
                if (isRunning) {
                    resumeFocusMode()
                }
            }
        }
        
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopFocusMode()
        Log.d(TAG, "FocusService destroyed")
    }

    private fun startFocusMode(durationMs: Long) {
        if (isRunning) {
            Log.d(TAG, "Focus mode already running")
            return
        }
        
        isRunning = true
        focusStartTimeMs = System.currentTimeMillis()
        focusDurationMs = durationMs
        
        // Activate NotificationListener
        NotificationListener.isFocusModeActive = true
        NotificationListener.notificationsSilenced = 0
        NotificationListener.repliesSent = 0
        
        // Reset reply cooldowns
        AutoReplyManager.reset()
        
        // Notify AutoReplyService about deep focus mode
        AutoReplyService.deepFocusActive = deepFocusEnabled
        
        // Enable Do Not Disturb if setting is on
        if (dndEnabled) {
            val dndResult = PermissionHelper.enableDnd(this)
            Log.d(TAG, "DND enabled: $dndResult")
        }
        
        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start periodic notification updates
        startNotificationUpdates()
        
        // Start timer if duration is set
        if (durationMs > 0) {
            startTimer(durationMs)
        }
        
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "FOCUS MODE ACTIVATED")
        Log.d(TAG, "Duration: ${durationMs / 1000 / 60} minutes")
        Log.d(TAG, "NotificationListener active: ${NotificationListener.isFocusModeActive}")
        Log.d(TAG, "AutoReplyService connected: ${AutoReplyService.isServiceConnected}")
        Log.d(TAG, "Deep Focus: $deepFocusEnabled")
        Log.d(TAG, "DND: $dndEnabled")
        Log.d(TAG, "═══════════════════════════════════════════")
    }
    
    private fun resumeFocusMode() {
        Log.d(TAG, "Resuming focus mode")
        
        // Check if timer has expired while we were dead
        if (focusDurationMs > 0) {
            val elapsed = System.currentTimeMillis() - focusStartTimeMs
            val remaining = focusDurationMs - elapsed
            
            if (remaining <= 0) {
                Log.d(TAG, "Timer expired while service was dead, stopping")
                stopFocusMode()
                return
            }
            
            // Resume timer with remaining time
            startTimer(remaining)
        }
        
        // Re-activate everything
        NotificationListener.isFocusModeActive = true
        AutoReplyService.deepFocusActive = deepFocusEnabled
        
        // Restart notification updates
        startForeground(NOTIFICATION_ID, createNotification())
        startNotificationUpdates()
    }

    private fun stopFocusMode() {
        if (!isRunning) {
            Log.d(TAG, "Focus mode not running")
            return
        }
        
        isRunning = false
        
        // Deactivate NotificationListener
        NotificationListener.isFocusModeActive = false
        
        // Deactivate deep focus
        AutoReplyService.deepFocusActive = false
        
        // Disable Do Not Disturb if we enabled it
        if (dndEnabled) {
            val dndResult = PermissionHelper.disableDnd(this)
            Log.d(TAG, "DND disabled: $dndResult")
        }
        
        // Stop updates and timer
        updateJob?.cancel()
        timerJob?.cancel()
        
        // Stop foreground
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        
        stopSelf()
        
        val duration = (System.currentTimeMillis() - focusStartTimeMs) / 1000 / 60
        
        // Reset times
        focusStartTimeMs = 0
        focusDurationMs = 0
        
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "FOCUS MODE ENDED")
        Log.d(TAG, "Duration: $duration minutes")
        Log.d(TAG, "Notifications silenced: ${NotificationListener.notificationsSilenced}")
        Log.d(TAG, "Replies sent: ${NotificationListener.repliesSent}")
        Log.d(TAG, "═══════════════════════════════════════════")
    }
    
    private fun startTimer(durationMs: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            delay(durationMs)
            Log.d(TAG, "Timer expired, stopping focus mode")
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
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val elapsed = (System.currentTimeMillis() - focusStartTimeMs) / 1000 / 60
        val stats = "${NotificationListener.notificationsSilenced} silenced · ${NotificationListener.repliesSent} replies"
        
        // Calculate time remaining if duration is set
        val timeText = if (focusDurationMs > 0) {
            val remaining = focusDurationMs - (System.currentTimeMillis() - focusStartTimeMs)
            val remainingMin = (remaining / 1000 / 60).coerceAtLeast(0)
            "${remainingMin}m left"
        } else {
            "${elapsed}m"
        }
        
        // Open app intent
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Stop focus intent
        val stopIntent = PendingIntent.getService(
            this,
            1,
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
                delay(60_000) // Update every minute
                
                if (isRunning) {
                    val manager = getSystemService(NotificationManager::class.java)
                    manager.notify(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }
}
