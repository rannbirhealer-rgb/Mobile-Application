package com.example.alarmboss.ui.ringing

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.alarm.AlarmScheduler
import com.example.alarmboss.alarm.FollowUp
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.StrictExerciseCategory
import com.example.alarmboss.data.UserProfile
import com.example.alarmboss.data.WakeEvent
import com.example.alarmboss.engine.ProgressionEngine
import com.example.alarmboss.engine.RingPlan
import com.example.alarmboss.engine.RingPlanner
import com.example.alarmboss.util.FollowUpSettings
import com.example.alarmboss.util.SnoozeSettings
import com.example.alarmboss.util.StreakManager
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Everything the ringing screen needs, resolved once when the ring starts. */
data class RingSession(
    val alarm: Alarm,
    val plan: RingPlan,
    val currentStreak: Int,
    val snoozeMinutes: Int,
    /** True when this ring is the post-dismissal "are you really up?" check, not the alarm itself. */
    val isWakeCheck: Boolean
)

class AlarmRingingViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as AlarmBossApp
    private val repository = app.repository
    private val profileRepository = app.profileRepository
    private val streakManager = StreakManager(application)
    private val snoozeSettings = SnoozeSettings(application)

    private val _session = MutableStateFlow<RingSession?>(null)
    val session: StateFlow<RingSession?> = _session

    // Snapshot taken before this wake-up is recorded, so boss day and difficulty stay stable
    // for the whole ring instead of shifting once the profile updates on dismiss.
    private var profileSnapshot: UserProfile? = null

    private val ringStartedAt: Long = System.currentTimeMillis()
    private var firstTouchAt: Long = 0L
    private var completed = false
    private var usedPuzzleAlternative = false

    fun load(id: Long, isWakeCheck: Boolean = false) {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            // Streak including today, fixed now so a dismiss after midnight can't change the plan.
            val projectedStreak = streakManager.projectedStreak()
            val alarm = repository.getAlarm(id) ?: return@launch
            profileSnapshot = profile
            _session.value = RingSession(
                alarm = alarm,
                plan = RingPlanner.plan(alarm.mode, profile.level, projectedStreak),
                currentStreak = streakManager.getCurrentStreak(),
                snoozeMinutes = snoozeSettings.minutes,
                isWakeCheck = isWakeCheck
            )
        }
    }

    fun onFirstTouch() {
        if (firstTouchAt == 0L) firstTouchAt = System.currentTimeMillis()
    }

    var lastWakeResult: StreakManager.StreakResult? = null
        private set

    /**
     * The user swapped a Strict workout for puzzles. The wake-up still counts, but is scored as the
     * mental challenge it actually was, so switching can't earn the larger physical-workout XP.
     */
    fun useStrictPuzzleAlternative() {
        usedPuzzleAlternative = true
    }

    /**
     * Records a genuine wake-up: the alarm's task (if any) was actually completed, not snoozed
     * away. Returns false if it was already recorded, so a double tap on the final step can't
     * award XP twice.
     */
    fun completeWakeUp(): Boolean {
        if (completed) return false
        completed = true
        cancelBackup()

        // Passing the wake-up check proves the user is still up, but it isn't a wake-up: no streak, no
        // XP, no event, and it must not queue another check behind itself.
        if (_session.value?.isWakeCheck == true) return true

        val nowTime = java.time.LocalTime.now()
        val streakResult = streakManager.recordSuccessfulWakeUpDetailed(today = LocalDate.now(), time = nowTime)
        lastWakeResult = streakResult
        val newStreak = streakResult.currentStreak
        val session = _session.value ?: return true
        scheduleWakeCheckIfEnabled(session.alarm)
        val profile = profileSnapshot ?: return true

        val alarm = session.alarm
        val plan = session.plan
        val category = if (usedPuzzleAlternative) StrictExerciseCategory.MENTAL else alarm.strictCategory
        val now = System.currentTimeMillis()
        val completionMs = now - ringStartedAt
        val reactionMs = (firstTouchAt.takeIf { it > 0L } ?: now) - ringStartedAt

        val outcome = ProgressionEngine.applyWakeUp(
            profile = profile,
            mode = alarm.mode,
            strictCategory = category.takeIf { alarm.mode == AlarmMode.STRICT },
            isBossFight = plan.isBossFight,
            newStreak = newStreak,
            reactionMs = reactionMs,
            completionMs = completionMs,
            today = LocalDate.now()
        )
        val event = WakeEvent(
            alarmId = alarm.id,
            triggeredAt = ringStartedAt,
            dismissedAt = now,
            challengeType = RingPlanner.challengeType(alarm.mode, category, plan),
            difficulty = plan.difficulty,
            xpEarned = outcome.xpEarned,
            reactionTimeMs = reactionMs,
            completionTimeMs = completionMs,
            isBossFight = plan.isBossFight
        )

        // App scope, not viewModelScope: the screen finishes right after this and would cancel it.
        // The next occurrence (or switching a one-off off) is settled when the alarm starts ringing,
        // by AlarmService, so it survives the screen or process dying mid-ring; nothing to redo here.
        app.appScope.launch { profileRepository.recordWakeUp(event, outcome.profile) }
        return true
    }

    /**
     * The commonest way a strict alarm fails: you pass the task, then go straight back to sleep. When
     * the wake-up check applies, ring again shortly after with a quick challenge. It applies to an
     * alarm that asks for it, to every Medium and Strict alarm, and to every non-Easy alarm when the
     * global setting is on. An Easy alarm only gets one if that alarm opted in.
     */
    private fun scheduleWakeCheckIfEnabled(alarm: Alarm) {
        val settings = FollowUpSettings(app)
        val applies = alarm.isWakeCheckEffective || (settings.wakeCheckEnabled && alarm.mode != AlarmMode.EASY)
        if (!applies) return
        AlarmScheduler(app).scheduleFollowUp(alarm.id, FollowUp.WAKE_CHECK, settings.wakeCheckMinutes)
    }

    /**
     * The safety valve for an alarm whose challenge genuinely can't be completed (illness, injury,
     * a camera that fails). It ends the ring but deliberately records nothing: no streak, no XP, no
     * wake event, so it can't be used as a cheaper way to earn credit. Returns false if the ring was
     * already finished.
     */
    fun emergencyStop(): Boolean {
        if (completed) return false
        completed = true
        cancelBackup()
        return true
    }

    /** Only offered in Easy mode: re-fires the alarm after the chosen snooze length, with a snooze XP penalty. */
    fun snooze() {
        val session = _session.value ?: return
        // The snooze is itself the next ring; a backup on top of it would ring twice.
        cancelBackup()
        AlarmScheduler(app).scheduleOneOffInMinutes(session.alarm, session.snoozeMinutes)
        app.appScope.launch {
            val currentProfile = profileRepository.getProfile()
            val penalizedProfile = currentProfile.copy(
                totalXp = kotlin.math.max(0, currentProfile.totalXp - 15)
            )
            profileRepository.updateProfile(penalizedProfile)
        }
    }

    fun addQuestBonusXp(bonusXp: Int) {
        if (bonusXp <= 0) return
        app.appScope.launch {
            val currentProfile = profileRepository.getProfile()
            val newTotalXp = currentProfile.totalXp + bonusXp
            val updated = currentProfile.copy(
                totalXp = newTotalXp,
                level = com.example.alarmboss.engine.XpEngine.calculateLevel(newTotalXp)
            )
            profileRepository.updateProfile(updated)
        }
    }

    /** The ring is being ended on purpose, so the safety-net ring behind it is no longer needed. */
    private fun cancelBackup() {
        val alarm = _session.value?.alarm ?: return
        AlarmScheduler(app).cancelFollowUp(alarm.id, FollowUp.BACKUP)
    }
}
