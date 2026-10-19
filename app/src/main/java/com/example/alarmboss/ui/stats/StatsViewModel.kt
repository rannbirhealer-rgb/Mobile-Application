package com.example.alarmboss.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarmboss.data.ProfileRepository
import com.example.alarmboss.data.UserProfile
import com.example.alarmboss.data.WakeEvent
import com.example.alarmboss.util.StreakManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.alarmboss.R

data class AvatarBadge(
    val id: String,
    val nameRes: Int,
    val minLevel: Int,
    val emoji: String
)

val AVAILABLE_AVATARS = listOf(
    AvatarBadge("ROOKIE", R.string.avatar_rookie, 1, "🌱"),
    AvatarBadge("WARRIOR", R.string.avatar_warrior, 2, "⚔️"),
    AvatarBadge("PALADIN", R.string.avatar_paladin, 5, "🛡️"),
    AvatarBadge("OVERLORD", R.string.avatar_overlord, 10, "👑")
)

class StatsViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    val profile: StateFlow<UserProfile> = profileRepository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserProfile()
    )

    /** Same rule as the alarm list banner, so both screens always show the same streak. */
    val activeStreak: StateFlow<Int> = profile
        .map { StreakManager.activeStreak(it.currentStreak, StreakManager.parseDate(it.lastWakeDate), LocalDate.now()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val wakeEvents: StateFlow<List<WakeEvent>> = profileRepository.observeWakeEvents().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getWakeDates(events: List<WakeEvent>): Set<LocalDate> {
        return events.map { 
            Instant.ofEpochMilli(it.dismissedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    fun selectAvatar(avatarId: String) {
        viewModelScope.launch {
            val current = profile.value
            val unlockedSet = current.unlockedAvatars.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            unlockedSet.add(avatarId)
            profileRepository.updateProfile(
                current.copy(
                    selectedAvatar = avatarId,
                    unlockedAvatars = unlockedSet.joinToString(",")
                )
            )
        }
    }
}
