package com.example.alarmboss.engine

import com.example.alarmboss.data.AlarmMode
import com.example.alarmboss.data.StrictExerciseCategory
import java.util.ArrayDeque
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

    @Test
    fun `level thresholds follow the square-root curve`() {
        assertEquals(1, XpEngine.calculateLevel(0))
        assertEquals(1, XpEngine.calculateLevel(49))
        assertEquals(2, XpEngine.calculateLevel(50))
        assertEquals(3, XpEngine.calculateLevel(200))
    }

    @Test
    fun `easy wake-up earns base xp plus streak bonus`() {
        assertEquals(10 + 5 * 3, XpEngine.calculateXp(AlarmMode.EASY, false, 3, 60_000))
    }

    @Test
    fun `fast completion adds the speed bonus`() {
        assertEquals(25 + 15, XpEngine.calculateXp(AlarmMode.MEDIUM, false, 0, 10_000))
    }

    @Test
    fun `physical strict pays more than mental strict`() {
        val physical = XpEngine.calculateXp(AlarmMode.STRICT, false, 0, 60_000, strictCategory = StrictExerciseCategory.PHYSICAL)
        val mental = XpEngine.calculateXp(AlarmMode.STRICT, false, 0, 60_000, strictCategory = StrictExerciseCategory.MENTAL)
        assertEquals(50, physical)
        assertEquals(40, mental)
    }

    @Test
    fun `boss fight is a flat 100 regardless of streak`() {
        assertEquals(100, XpEngine.calculateXp(AlarmMode.EASY, true, 30, 1_000))
    }

    @Test
    fun `difficulty is clamped between 1 and 10`() {
        assertEquals(1, DifficultyEngine.calculate(level = 1, streak = 0))
        assertEquals(10, DifficultyEngine.calculate(level = 100, streak = 100))
    }

    @Test
    fun `difficulty grows with level and streak`() {
        assertEquals(4, DifficultyEngine.calculate(level = 6, streak = 10))
    }

    @Test
    fun `boss fight only on every seventh streak day`() {
        assertFalse(BossFightManager.isBossFightDay(com.example.alarmboss.data.UserProfile(currentStreak = 0)))
        assertFalse(BossFightManager.isBossFightDay(com.example.alarmboss.data.UserProfile(currentStreak = 6)))
        assertTrue(BossFightManager.isBossFightDay(com.example.alarmboss.data.UserProfile(currentStreak = 7)))
        assertTrue(BossFightManager.isBossFightDay(com.example.alarmboss.data.UserProfile(currentStreak = 14)))
    }

    @Test
    fun `generated maze has a start and a reachable exit`() {
        repeat(25) {
            val maze = MazeGenerator.generate(9, 9)
            assertEquals(2, maze[1][1])
            assertEquals(3, maze[7][7])
            assertTrue(exitReachable(maze))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `even maze dimensions are rejected`() {
        MazeGenerator.generate(8, 8)
    }

    private fun exitReachable(maze: Array<IntArray>): Boolean {
        val seen = Array(maze.size) { BooleanArray(maze[0].size) }
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(1 to 1)
        seen[1][1] = true
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            if (maze[r][c] == 3) return true
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr
                val nc = c + dc
                if (nr in maze.indices && nc in maze[0].indices && !seen[nr][nc] && maze[nr][nc] != 1) {
                    seen[nr][nc] = true
                    queue.add(nr to nc)
                }
            }
        }
        return false
    }
}
