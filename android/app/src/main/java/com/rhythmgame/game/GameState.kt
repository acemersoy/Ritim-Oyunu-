package com.rhythmgame.game

import com.rhythmgame.data.model.Note
import java.util.Random

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
    // Pre-computed spark positions to avoid Math.random() in render loop
    val sparkOffsetsX: FloatArray = FloatArray(5),
    val sparkOffsetsY: FloatArray = FloatArray(5),
) {
    companion object {
        private val random = Random()

        fun create(lane: Int, result: HitResult, createdAt: Long, y: Float): HitEffect {
            val offsetsX = FloatArray(5) { (random.nextFloat() - 0.5f) }
            val offsetsY = FloatArray(5) { random.nextFloat() }
            return HitEffect(lane, result, createdAt, y, offsetsX, offsetsY)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HitEffect) return false
        return lane == other.lane && result == other.result && createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var h = lane
        h = 31 * h + result.hashCode()
        h = 31 * h + createdAt.hashCode()
        return h
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var color: Int,
    var size: Float,
    var rotation: Float = 0f,
    var textureIndex: Int = 0,
    var laneIndex: Int = 0,
) {
    fun reset(
        x: Float, y: Float, vx: Float, vy: Float, life: Float, color: Int, size: Float,
        rotation: Float = 0f, textureIndex: Int = 0, laneIndex: Int = 0,
    ) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy
        this.life = life; this.color = color; this.size = size
        this.rotation = rotation; this.textureIndex = textureIndex; this.laneIndex = laneIndex
    }
}

/**
 * Pool for reusing Particle objects to reduce GC pressure.
 */
class ParticlePool(private val maxSize: Int = 300) {
    private val pool = ArrayDeque<Particle>(maxSize)

    fun obtain(
        x: Float, y: Float, vx: Float, vy: Float, life: Float, color: Int, size: Float,
        rotation: Float = 0f, textureIndex: Int = 0, laneIndex: Int = 0,
    ): Particle {
        val p = if (pool.isNotEmpty()) pool.removeLast() else Particle(0f, 0f, 0f, 0f, 0f, 0, 0f)
        p.reset(x, y, vx, vy, life, color, size, rotation, textureIndex, laneIndex)
        return p
    }

    fun recycle(particle: Particle) {
        if (pool.size < maxSize) {
            pool.addLast(particle)
        }
    }
}

data class GameState(
    val phase: GamePhase = GamePhase.LOADING,
    val currentTimeMs: Long = 0,
    val frameTimeMs: Long = 0,
    val score: Int = 0,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val perfectCount: Int = 0,
    val greatCount: Int = 0,
    val goodCount: Int = 0,
    val missCount: Int = 0,
    val overpressCount: Int = 0,
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
