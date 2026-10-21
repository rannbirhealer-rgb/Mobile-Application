package com.example.alarmboss.alarm

/**
 * Extra rings tied to a main alarm. Each kind gets its own PendingIntent request-code range, so
 * scheduling one never replaces the alarm itself (or another kind): PendingIntents are identified by
 * request code, and reusing the alarm's own would silently overwrite its next occurrence.
 */
enum class FollowUp(val requestOffset: Int) {
    /** A short "are you really up?" challenge a few minutes after the user finished the alarm. */
    WAKE_CHECK(1_000_000),

    /**
     * A safety net: rings again if the alarm still hasn't been completed after a set time, for a ring
     * that was missed, silenced by mistake, or lost when the screen or service was killed. Cancelled
     * the moment the user finishes, snoozes or emergency-stops.
     */
    BACKUP(2_000_000)
}
