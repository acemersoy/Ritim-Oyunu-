package com.rhythmgame.game

class ScoreManager {
    private var score = 0
    private var combo = 0
    private var maxCombo = 0
    private var perfectCount = 0
    private var greatCount = 0
    private var goodCount = 0
    private var missCount = 0
    private var _powerMultiplier = 1f

    val comboMultiplier: Float
        get() = 1.0f + (combo / 10) * 0.1f

    @Synchronized
    fun onHit(result: HitResult): Int {
        val basePoints = when (result) {
            HitResult.PERFECT -> 300
            HitResult.GREAT -> 200
            HitResult.GOOD -> 100
            HitResult.MISS -> 0
        }

        if (result == HitResult.MISS) {
            combo = 0
            missCount++
            return 0
        }

        combo++
        if (combo > maxCombo) maxCombo = combo

        when (result) {
            HitResult.PERFECT -> perfectCount++
            HitResult.GREAT -> greatCount++
            HitResult.GOOD -> goodCount++
            else -> {}
        }

        val multiplier = if (_powerMultiplier > 1f) _powerMultiplier else comboMultiplier.coerceAtMost(4.0f)
        val points = (basePoints * multiplier).toInt()
        score += points
        return points
    }

    fun getScore() = score
    fun getCombo() = combo
    fun getMaxCombo() = maxCombo
    fun getPerfectCount() = perfectCount
    fun getGreatCount() = greatCount
    fun getGoodCount() = goodCount
    fun getMissCount() = missCount
    fun setPowerMultiplier(multiplier: Float) { _powerMultiplier = multiplier }
    fun getPowerMultiplier() = _powerMultiplier

    fun reset() {
        score = 0
        combo = 0
        maxCombo = 0
        perfectCount = 0
        greatCount = 0
        goodCount = 0
        missCount = 0
        _powerMultiplier = 1f
    }
}
