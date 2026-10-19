package com.example.alarmboss.ui.settings

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarmboss.AlarmBossApp
import com.example.alarmboss.alarm.AlarmRescheduler
import com.example.alarmboss.data.ContentStore
import com.example.alarmboss.engine.HolidayRegion
import com.example.alarmboss.util.AccessibilitySettings
import com.example.alarmboss.util.FollowUpSettings
import com.example.alarmboss.util.HolidaySettings
import com.example.alarmboss.util.SnoozeSettings
import com.example.alarmboss.util.ThemeMode
import com.example.alarmboss.util.ThemeSettings
import com.example.alarmboss.util.SoundSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Reading passages are separated by a line containing three dashes, one passage per block. */
internal fun parsePassages(text: String): List<String> =
    text.split("---").map { it.trim() }.filter { it.isNotBlank() }

internal fun formatPassages(passages: List<String>): String = passages.joinToString("\n---\n")

/** Lyric lines are one per line; blank lines are ignored. */
internal fun parseLyrics(text: String): List<String> =
    text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

internal fun formatLyrics(lines: List<String>): String = lines.joinToString("\n")

/**
 * Holds the editable drafts for the Settings screen. Drafts live here, not in the composable, so
 * typing survives rotation, and the stores are only touched when the user saves.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ContentStore(application)
    private val snoozeSettings = SnoozeSettings(application)

    private val _snoozeMinutes = MutableStateFlow(snoozeSettings.minutes)
    val snoozeMinutes: StateFlow<Int> = _snoozeMinutes

    private val soundSettings = SoundSettings(application)

    private val _loudness = MutableStateFlow(soundSettings.loudnessPercent)
    val loudness: StateFlow<Int> = _loudness

    private val _gradualVolume = MutableStateFlow(soundSettings.gradual)
    val gradualVolume: StateFlow<Boolean> = _gradualVolume

    private val _raiseSystemVolume = MutableStateFlow(soundSettings.raiseSystemVolume)
    val raiseSystemVolume: StateFlow<Boolean> = _raiseSystemVolume

    private val accessibilitySettings = AccessibilitySettings(application)

    private val _silentMode = MutableStateFlow(accessibilitySettings.silentMode)
    val silentMode: StateFlow<Boolean> = _silentMode

    private val _flash = MutableStateFlow(accessibilitySettings.flash)
    val flash: StateFlow<Boolean> = _flash

    private val followUpSettings = FollowUpSettings(application)

    private val _wakeCheckEnabled = MutableStateFlow(followUpSettings.wakeCheckEnabled)
    val wakeCheckEnabled: StateFlow<Boolean> = _wakeCheckEnabled

    private val _wakeCheckMinutes = MutableStateFlow(followUpSettings.wakeCheckMinutes)
    val wakeCheckMinutes: StateFlow<Int> = _wakeCheckMinutes

    private val _backupMinutes = MutableStateFlow(followUpSettings.backupMinutes)
    val backupMinutes: StateFlow<Int> = _backupMinutes

    private val themeSettings = ThemeSettings(application)

    private val _themeMode = MutableStateFlow(themeSettings.mode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        themeSettings.mode = mode
        _themeMode.value = mode
    }

    private val holidaySettings = HolidaySettings(application)
    private val rescheduler = AlarmRescheduler(application, (application as AlarmBossApp).repository)

    private val _holidayEnabled = MutableStateFlow(holidaySettings.enabled)
    val holidayEnabled: StateFlow<Boolean> = _holidayEnabled

    private val _holidayRegion = MutableStateFlow(holidaySettings.region)
    val holidayRegion: StateFlow<HolidayRegion> = _holidayRegion

    private val _customDays = MutableStateFlow(holidaySettings.customDates.sorted())
    val customDays: StateFlow<List<LocalDate>> = _customDays

    private val _skippedDays = MutableStateFlow(upcomingSkips())
    val skippedDays: StateFlow<List<LocalDate>> = _skippedDays

    // A skip can be agreed from the alarm list while this screen is alive, so follow the stored value.
    private val skipListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "holiday_skipped_dates") _skippedDays.value = upcomingSkips()
    }

    private val _passagesText = MutableStateFlow("")
    val passagesText: StateFlow<String> = _passagesText

    private val _lyricsText = MutableStateFlow("")
    val lyricsText: StateFlow<String> = _lyricsText

    private val counselorEngine = com.example.alarmboss.counselor.SleepCounselorEngine(application)
    private val _isGeneratingPassages = MutableStateFlow(false)
    val isGeneratingPassages: StateFlow<Boolean> = _isGeneratingPassages

    private val _barcodeText = MutableStateFlow("")
    val barcodeText: StateFlow<String> = _barcodeText

    init {
        application.getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(skipListener)
        // Seed the drafts once from what's stored. Reading continuously would overwrite
        // whatever the user is in the middle of typing.
        viewModelScope.launch {
            _passagesText.value = formatPassages(store.readingPassages.first())
            _lyricsText.value = formatLyrics(store.lyricLines.first())
            _barcodeText.value = store.getRegisteredBarcode() ?: ""
        }
    }

    override fun onCleared() {
        getApplication<Application>().getSharedPreferences("alarm_boss_settings", Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(skipListener)
    }

    fun setSnoozeMinutes(minutes: Int) {
        snoozeSettings.minutes = minutes
        _snoozeMinutes.value = snoozeSettings.minutes
    }

    fun setLoudness(percent: Int) {
        soundSettings.loudnessPercent = percent
        _loudness.value = soundSettings.loudnessPercent
    }

    fun setGradualVolume(enabled: Boolean) {
        soundSettings.gradual = enabled
        _gradualVolume.value = enabled
    }

    fun setRaiseSystemVolume(enabled: Boolean) {
        soundSettings.raiseSystemVolume = enabled
        _raiseSystemVolume.value = enabled
    }

    fun setSilentMode(enabled: Boolean) {
        accessibilitySettings.silentMode = enabled
        _silentMode.value = enabled
    }

    fun setFlash(enabled: Boolean) {
        accessibilitySettings.flash = enabled
        _flash.value = enabled
    }

    fun setWakeCheckEnabled(enabled: Boolean) {
        followUpSettings.wakeCheckEnabled = enabled
        _wakeCheckEnabled.value = enabled
    }

    fun setWakeCheckMinutes(minutes: Int) {
        followUpSettings.wakeCheckMinutes = minutes
        _wakeCheckMinutes.value = followUpSettings.wakeCheckMinutes
    }

    fun setBackupMinutes(minutes: Int) {
        followUpSettings.backupMinutes = minutes
        _backupMinutes.value = followUpSettings.backupMinutes
    }

    fun setHolidayEnabled(enabled: Boolean) {
        holidaySettings.enabled = enabled
        _holidayEnabled.value = enabled
        reschedule()
    }

    fun setHolidayRegion(region: HolidayRegion) {
        holidaySettings.region = region
        _holidayRegion.value = region
    }

    fun addCustomDay(date: LocalDate) {
        holidaySettings.addCustomDate(date)
        _customDays.value = holidaySettings.customDates.sorted()
    }

    /** Removing a day off also undoes any skip already agreed for it. */
    fun removeCustomDay(date: LocalDate) {
        holidaySettings.removeCustomDate(date)
        _customDays.value = holidaySettings.customDates.sorted()
        cancelSkip(date)
    }

    /** Changes a confirmed skip back to ringing. Ringing more is always safe, so this is never gated. */
    fun cancelSkip(date: LocalDate) {
        holidaySettings.keepRinging(date)
        _skippedDays.value = upcomingSkips()
        reschedule()
    }

    private fun upcomingSkips(): List<LocalDate> =
        holidaySettings.skippedDates.filter { !it.isBefore(LocalDate.now()) }.sorted()

    // Alarms already queued with the system hold absolute times, so they need lining up again.
    private fun reschedule() {
        viewModelScope.launch { rescheduler.rescheduleAll() }
    }

    fun onPassagesChange(text: String) { _passagesText.value = text }
    fun onLyricsChange(text: String) { _lyricsText.value = text }
    fun onBarcodeChange(text: String) { _barcodeText.value = text }

    fun savePassages() {
        val passages = parsePassages(_passagesText.value)
        viewModelScope.launch { store.setReadingPassages(passages) }
    }

    fun saveLyrics() {
        val lines = parseLyrics(_lyricsText.value)
        viewModelScope.launch { store.setLyricLines(lines) }
    }

    fun saveBarcode() {
        viewModelScope.launch { store.setRegisteredBarcode(_barcodeText.value) }
    }

    private val _hasCounselorKey = MutableStateFlow(counselorEngine.hasApiKey())
    val hasCounselorKey: StateFlow<Boolean> = _hasCounselorKey

    private val _counselorKeyDraft = MutableStateFlow("")
    val counselorKeyDraft: StateFlow<String> = _counselorKeyDraft

    /** Why the last "AI Generate" produced nothing; shown once, then cleared. */
    private val _generationFailed = MutableStateFlow(false)
    val generationFailed: StateFlow<Boolean> = _generationFailed

    fun onCounselorKeyChange(text: String) { _counselorKeyDraft.value = text }

    fun saveCounselorKey() {
        counselorEngine.saveApiKey(_counselorKeyDraft.value)
        _counselorKeyDraft.value = ""
        _hasCounselorKey.value = counselorEngine.hasApiKey()
    }

    fun clearCounselorKey() {
        counselorEngine.clearApiKey()
        _hasCounselorKey.value = false
    }


    /**
     * Asks the AI for fresh reading passages and adds them to the end of the user's own. They are
     * never replaced: a tap here must not wipe passages someone wrote or chose, and without a key or
     * a connection there is simply nothing to add, which is reported instead of faked.
     */
    fun generatePassagesWithGemini() {
        if (_isGeneratingPassages.value || !counselorEngine.hasApiKey()) return
        viewModelScope.launch {
            _isGeneratingPassages.value = true
            _generationFailed.value = false
            try {
                val generated = counselorEngine.generateReadingPassages()
                val existing = _passagesText.value.trim()
                _passagesText.value = if (existing.isEmpty()) generated else existing + "\n---\n" + generated
                savePassages()
            } catch (e: Exception) {
                _generationFailed.value = true
            } finally {
                _isGeneratingPassages.value = false
            }
        }
    }
}
