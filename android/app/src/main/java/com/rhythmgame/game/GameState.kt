package com.rhythmgame.game

import com.rhythmgame.data.model.Note

enum class HitResult {
    PERFECT, GREAT, GOOD, MISS
}

enum class GamePhase {
    LOADING, COUNTDOWN, PLAYING, PAUSED, FINISHED
}

data class ActiveNote(
    val note: Note,
    var y: Float = -100f,
    var isHit: Boolean = false,
    var hitResult: HitResult? = null,
    var isActive: Boolean = true,
    var holdProgress: Float = 0f,
    var missReported: Boolean = false,
)

data class HitEffect(
    val lane: Int,
    val result: HitResult,
    val createdAt: Long,
    val y: Float,
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float, // 1.0 = just born, 0.0 = dead
    var color: Int,
    var size: Float,
)

data class GameState(
    val phase: GamePhase = GamePhase.LOADING,
    val currentTimeMs: Long = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val perfectCount: Int = 0,
    val greatCount: Int = 0,
    val goodCount: Int = 0,
    val missCount: Int = 0,
    val totalNotes: Int = 0,
    val comboMultiplier: Float = 1.0f,
    val activeNotes: List<ActiveNote> = emptyList(),
    val hitEffects: List<HitEffect> = emptyList(),
    val countdownValue: Int = 3,
    val pressedLanes: Set<Int> = emptySet(),
    val particles: List<Particle> = emptyList(),
    val songDurationMs: Long = 0,
    val powerBarProgress: Float = 0f,
    val powerActive: Boolean = false,
    val powerMultiplier: Float = 1f,
    val powerRemainingMs: Long = 0,
    val powerDurationMs: Long = 0,
) {
    val accuracy: Float
        get() {
            val total = perfectCount + greatCount + goodCount + missCount
            if (total == 0) return 0f
            return (perfectCount * 100f + greatCount * 75f + goodCount * 50f) / (total * 100f) * 100f
        }

    val hitNotes: Int get() = perfectCount + greatCount + goodCount
}
