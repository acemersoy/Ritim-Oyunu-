package com.rhythmgame.game

class ScoreManager {
    private var score = 0
    private var combo = 0
    private var maxCombo = 0
    private var perfectCount = 0
    private var greatCount = 0
    private var goodCount = 0
    private var missCount = 0
    private var overpressCount = 0
    private var _powerMultiplier = 1f

    /** Discrete combo tiers: 1x / 2x / 3x / 4x */
    val comboMultiplier: Float
        get() = when {
            combo >= 50 -> 4f
            combo >= 30 -> 3f
            combo >= 10 -> 2f
            else -> 1f
        }

    /** Effective multiplier shown to player: combo tier * power multiplier */
    val totalMultiplier: Float
        get() = comboMultiplier * _powerMultiplier

    @Synchronized
    fun onHit(result: HitResult): Int {
        val basePoints = when (result) {
            HitResult.PERFECT -> 3
            HitResult.GREAT -> 2
            HitResult.GOOD -> 1
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

        // Combo tier * power multiplier stack multiplicatively
        // e.g. 3x combo * 4x power = 12x total
        val points = (basePoints * totalMultiplier).toInt()
        score += points
        return points
    }

    @Synchronized
    fun onOverpress() {
        combo = 0
        overpressCount++
    }

    fun getScore() = score
    fun getCombo() = combo
    fun getMaxCombo() = maxCombo
    fun getPerfectCount() = perfectCount
    fun getGreatCount() = greatCount
    fun getGoodCount() = goodCount
    fun getMissCount() = missCount
    fun getOverpressCount() = overpressCount
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
        overpressCount = 0
        _powerMultiplier = 1f
    }
}
