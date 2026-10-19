package com.example.alarmboss.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Singleton profile
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalWakeUps: Int = 0,
    val bossesDefeated: Int = 0,
    val lastWakeDate: String? = null,
    val bedtimeGoal: String? = null,
    val averageReactionMs: Long = 0,
    val unlockedAvatars: String = "ROOKIE",
    val selectedAvatar: String = "ROOKIE"
)
