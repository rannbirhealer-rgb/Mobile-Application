package com.example.alarmboss.engine

import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.UserProfile
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionEngineTest {

    private val today = LocalDate.of(2026, 10, 15)

    private fun wake(
        profile: UserProfile,
        newStreak: Int,
        boss: Boolean = false,
        reaction: Long = 4_000,
        completion: Long = 60_000
    ) = ProgressionEngine.applyWakeUp(
        profile = profile,
        mode = AlarmMode.EASY,
        strictCategory = null,
        isBossFight = boss,
        newStreak = newStreak,
        reactionMs = reaction,
        completionMs = completion,
        today = today
    )

    @Test
    fun `first wake-up awards xp and starts the records`() {
        val outcome = wake(UserProfile(), newStreak = 1)
        assertEquals(15, outcome.xpEarned)
        assertEquals(15, outcome.profile.totalXp)
        assertEquals(1, outcome.profile.totalWakeUps)
        assertEquals(1, outcome.profile.currentStreak)
        assertEquals(1, outcome.profile.longestStreak)
        assertEquals(today.toString(), outcome.profile.lastWakeDate)
    }

    @Test
    fun `level rises when enough xp accumulates`() {
        val outcome = wake(UserProfile(totalXp = 40, level = 1), newStreak = 1)
        assertEquals(55, outcome.profile.totalXp)
        assertEquals(2, outcome.profile.level)
    }

    @Test
    fun `longest streak never shrinks when the streak resets`() {
        val outcome = wake(UserProfile(currentStreak = 12, longestStreak = 12), newStreak = 1)
        assertEquals(1, outcome.profile.currentStreak)
        assertEquals(12, outcome.profile.longestStreak)
    }

    @Test
    fun `boss fight pays flat xp and counts as a defeat`() {
        val outcome = wake(UserProfile(bossesDefeated = 2), newStreak = 7, boss = true)
        assertEquals(100, outcome.xpEarned)
        assertEquals(3, outcome.profile.bossesDefeated)
    }

    @Test
    fun `reaction time is a running average`() {
        val outcome = wake(UserProfile(totalWakeUps = 3, averageReactionMs = 2_000), newStreak = 4, reaction = 10_000)
        assertEquals(4_000, outcome.profile.averageReactionMs)
    }

    @Test
    fun `avatar badges unlock when level requirements are met`() {
        val outcome = wake(UserProfile(totalXp = 40, level = 1), newStreak = 1)
        assertEquals(2, outcome.profile.level)
        val avatars = outcome.profile.unlockedAvatars.split(",")
        org.junit.Assert.assertTrue(avatars.contains("ROOKIE"))
        org.junit.Assert.assertTrue(avatars.contains("WARRIOR"))
    }
}
