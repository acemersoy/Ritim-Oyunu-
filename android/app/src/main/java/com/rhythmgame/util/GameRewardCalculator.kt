package com.rhythmgame.util

object GameRewardCalculator {
    fun totalXpForLevel(level: Int): Long =
        100L * level * level + 200L * level + 200L

    fun levelFromTotalXp(totalXp: Long): Int {
        var level = 0
        while (totalXpForLevel(level + 1) <= totalXp) level++
        return level
    }

    fun xpProgress(totalXp: Long, currentLevel: Int): Float {
        val cur = totalXpForLevel(currentLevel)
        val next = totalXpForLevel(currentLevel + 1)
        return ((totalXp - cur).toFloat() / (next - cur).toFloat()).coerceIn(0f, 1f)
    }

    fun starsForGame(difficulty: String, durationMs: Int?): Int {
        val isShort = (durationMs ?: 0) <= 60_000
        return when (difficulty) {
            "easy" -> if (isShort) 0 else 1
            "medium" -> if (isShort) 2 else 3
            "hard" -> if (isShort) 3 else 5
            else -> 1
        }
    }

    fun xpForGame(accuracyPercent: Float, stars: Int): Long =
        (accuracyPercent * stars).toLong()

    fun coinsForGame(difficulty: String, durationMs: Int, noteCount: Int): Int {
        val diffMultiplier = when (difficulty) {
            "easy" -> 0f
            "medium" -> 1.0f
            "hard" -> 1.5f
            else -> 1.0f
        }
        if (diffMultiplier == 0f) return 0
        val durationSec = durationMs / 1000f
        return kotlin.math.floor((durationSec / 30f) * (noteCount / 50f) * diffMultiplier)
            .toInt()
            .coerceAtMost(30)
    }
}
