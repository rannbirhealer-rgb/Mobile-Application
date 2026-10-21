package com.example.alarmboss.ui.alarms

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.alarm.AlarmScheduler
import com.example.alarmboss.data.Alarm
import com.example.alarmboss.data.LOCK_WINDOW
import com.example.alarmboss.engine.HolidayCalendar
import com.example.alarmboss.engine.HolidayPlanner
import com.example.alarmboss.util.HolidaySettings
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.alarmboss.util.StreakManager

/** The result of looking up the alarm an edit screen was opened for. */
sealed interface EditTarget {
    data object Loading : EditTarget
    data object Missing : EditTarget
    data class Existing(val alarm: Alarm) : EditTarget
}

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val streakManager = StreakManager(application)

    private val repository = (application as AlarmBossApp).repository
    private val scheduler = AlarmScheduler(application)

    private val _streak = MutableStateFlow(streakManager.getCurrentStreak())
    val streak: StateFlow<Int> = _streak.asStateFlow()
    val alarms: StateFlow<List<Alarm>> = repository.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editTarget = MutableStateFlow<EditTarget>(EditTarget.Loading)
    val editTarget: StateFlow<EditTarget> = _editTarget.asStateFlow()
    private var requestedEditId: Long? = null

    private val holidaySettings = HolidaySettings(application)

    private val _skipDates = MutableStateFlow(holidaySettings.activeSkipDates)
    /** Days repeating alarms are stepping over; the list needs them to judge which alarms are locked. */
    val skipDates: StateFlow<Set<LocalDate>> = _skipDates.asStateFlow()

    private val _holidayPrompt = MutableStateFlow<HolidayPlanner.Prompt?>(null)
    /** The next holiday waiting for the user's yes or no, if any. */
    val holidayPrompt: StateFlow<HolidayPlanner.Prompt?> = _holidayPrompt.asStateFlow()

    // The settings screen changes these from elsewhere; listening keeps the list in step with it.
    private val holidayPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key?.startsWith("holiday_") == true) refreshHolidays()
    }

    init {
        application.getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(holidayPrefsListener)
        viewModelScope.launch { alarms.collect { refreshHolidays() } }
    }

    override fun onCleared() {
        getApplication<Application>().getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(holidayPrefsListener)
    }

    /** Recomputes the skipped days and whether there is a holiday to ask about. */
    fun refreshHolidays(now: LocalDateTime = LocalDateTime.now()) {
        val today = now.toLocalDate()
        holidaySettings.prune(today)
        _skipDates.value = holidaySettings.activeSkipDates
        _holidayPrompt.value = if (!holidaySettings.enabled) null else {
            val holidays = HolidayCalendar.between(
                holidaySettings.region, today, today.plusDays(HolidayPlanner.LOOKAHEAD_DAYS), holidaySettings.customDates
            )
            val slots = alarms.value.filter { it.isEnabled }.map { HolidayPlanner.Slot(it.hour, it.minute, it.repeatDays) }
            HolidayPlanner.pending(holidays, slots, holidaySettings.answeredDates, now, LOCK_WINDOW).firstOrNull()
        }
    }

    /** Records the user's answer. Only an explicit yes skips anything; both answers end the question. */
    fun answerHoliday(prompt: HolidayPlanner.Prompt, skip: Boolean) = viewModelScope.launch {
        if (skip) holidaySettings.skip(prompt.holiday.date) else holidaySettings.keepRinging(prompt.holiday.date)
        // Alarms already queued still point at the day just skipped, so line them up again.
        repository.getEnabledAlarms().forEach { scheduler.schedule(it) }
        refreshHolidays()
    }

    fun loadStreak() {
        _streak.value = streakManager.getCurrentStreak()
    }

    /**
     * Resolves the alarm to edit. Idempotent for the same id: the screen asks again after every
     * rotation, and answering "Loading" a second time would tear down a form the user is typing in.
     */
    fun loadForEdit(id: Long) {
        if (requestedEditId == id) return
        requestedEditId = id
        viewModelScope.launch {
            _editTarget.value = repository.getAlarm(id)?.let { EditTarget.Existing(it) } ?: EditTarget.Missing
        }
    }

    fun save(alarm: Alarm) = viewModelScope.launch {
        val id = repository.save(alarm)
        val saved = alarm.copy(id = if (alarm.id == 0L) id else alarm.id)
        if (saved.isEnabled) scheduler.schedule(saved) else scheduler.cancel(saved.id)
    }

    fun delete(alarm: Alarm) = viewModelScope.launch {
        scheduler.cancel(alarm.id)
        repository.delete(alarm)
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) = viewModelScope.launch {
        repository.setEnabled(alarm.id, enabled)
        if (enabled) scheduler.schedule(alarm.copy(isEnabled = true)) else scheduler.cancel(alarm.id)
    }
}
