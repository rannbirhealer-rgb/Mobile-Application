package com.example.alarmboss.engine

/** Decides which senses an alarm uses to wake the user. */
object AlarmFeedbackPlanner {

    data class Feedback(val sound: Boolean, val vibrate: Boolean)

    /**
     * Silent mode is for people who can't rely on hearing (or share a room): it drops the sound and
     * forces vibration on regardless of the alarm's own vibrate flag, because an alarm that did
     * neither would wake no one. Otherwise sound always plays and vibration follows the alarm.
     */
    fun plan(alarmVibrate: Boolean, silentMode: Boolean): Feedback =
        if (silentMode) Feedback(sound = false, vibrate = true) else Feedback(sound = true, vibrate = alarmVibrate)
}
