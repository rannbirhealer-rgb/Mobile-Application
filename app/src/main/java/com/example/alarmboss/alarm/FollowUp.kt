package com.example.alarmboss.alarm

/**
 * Extra rings tied to a main alarm. Each kind gets its own PendingIntent request-code range, so
 * scheduling one never replaces the alarm itself (or another kind): PendingIntents are identified by
 * request code, and reusing the alarm's own would silently overwrite its next occurrence.
 */
enum class FollowUp(val requestOffset: Int) {
    /** A short "are you really up?" challenge a few minutes after the user finished the alarm. */
    WAKE_CHECK(1_000_000)
}
