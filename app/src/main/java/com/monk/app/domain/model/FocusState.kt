package com.monk.app.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Represents the current state of focus mode
 */
data class FocusState(
    val isActive: Boolean = false,
    val startedAt: Instant? = null,
    val scheduledEndAt: Instant? = null, // null = indefinite
    val repliesSent: Int = 0,
    val notificationsSilenced: Int = 0,
    val tick: Long = 0 // Increments every second to force UI updates
) {
    /**
     * How long the current focus session has been active
     */
    val duration: Duration?
        get() = startedAt?.let { Duration.between(it, Instant.now()) }

    /**
     * Time remaining if a scheduled end time is set
     */
    val timeRemaining: Duration?
        get() = scheduledEndAt?.let { 
            val remaining = Duration.between(Instant.now(), it)
            if (remaining.isNegative) Duration.ZERO else remaining
        }

    /**
     * Whether the session should auto-end (scheduled end time passed)
     */
    val shouldAutoEnd: Boolean
        get() = scheduledEndAt?.let { Instant.now().isAfter(it) } ?: false

    companion object {
        val INACTIVE = FocusState(isActive = false)
    }
}

/**
 * Focus session duration presets
 */
enum class FocusDuration(
    val displayName: String,
    val duration: Duration?,
    val isCustom: Boolean = false
) {
    MINUTES_30("30m", Duration.ofMinutes(30)),
    HOUR_1("1h", Duration.ofHours(1)),
    HOURS_2("2h", Duration.ofHours(2)),
    CUSTOM("Custom", null, true),
    INDEFINITE("∞", null);
}

/**
 * Holds custom duration value set by user
 */
object CustomDuration {
    var minutes: Int = 60
    
    fun getDuration(): Duration = Duration.ofMinutes(minutes.toLong())
}
