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
    val notificationsSilenced: Int = 0
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
    val duration: Duration?
) {
    MINUTES_15("15 min", Duration.ofMinutes(15)),
    MINUTES_30("30 min", Duration.ofMinutes(30)),
    MINUTES_45("45 min", Duration.ofMinutes(45)),
    HOUR_1("1 hour", Duration.ofHours(1)),
    HOURS_2("2 hours", Duration.ofHours(2)),
    HOURS_4("4 hours", Duration.ofHours(4)),
    INDEFINITE("Until I stop", null);
}
