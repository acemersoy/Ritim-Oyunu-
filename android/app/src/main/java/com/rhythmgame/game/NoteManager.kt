package com.rhythmgame.game

import com.rhythmgame.data.model.Note

class NoteManager(
    private val notes: List<Note>,
    private val hitLineY: Float,
    private val screenHeight: Float,
    private val approachTimeMs: Long,
) {
    private var nextNoteIndex = 0
    private val activeNotes = mutableListOf<ActiveNote>()
    private val lock = Any()

    companion object {
        const val PERFECT_WINDOW_MS = 100L
        const val GREAT_WINDOW_MS = 200L
        const val GOOD_WINDOW_MS = 300L
        const val MISS_THRESHOLD_MS = 350L
    }

    fun update(currentTimeMs: Long): List<ActiveNote> {
        synchronized(lock) {
            // Add notes that should appear on screen
            while (nextNoteIndex < notes.size) {
                val note = notes[nextNoteIndex]
                val noteAppearTime = note.timeMs - approachTimeMs
                if (currentTimeMs >= noteAppearTime) {
                    activeNotes.add(ActiveNote(note = note))
                    nextNoteIndex++
                } else {
                    break
                }
            }

            // Update positions for all active notes
            for (activeNote in activeNotes) {
                val noteTimeMs = activeNote.note.timeMs
                val progress = (currentTimeMs - (noteTimeMs - approachTimeMs)).toFloat() / approachTimeMs.toFloat()
                activeNote.y = progress * hitLineY

                // Mark missed notes (tap notes or unheld hold notes)
                // Skip notes that are actively being held — they're scored on release
                if (!activeNote.isHit && !activeNote.isActive && activeNote.hitResult == null && currentTimeMs > noteTimeMs + MISS_THRESHOLD_MS) {
                    activeNote.isHit = true
                    activeNote.hitResult = HitResult.MISS
                }
            }

            // Remove notes that have scrolled past and been resolved
            activeNotes.removeAll { note ->
                val isResolved = note.isHit || note.hitResult == HitResult.MISS
                isResolved && note.y > hitLineY + 800f
            }

            // Return direct reference - caller must not modify
            return activeNotes.toList()
        }
    }

    /**
     * Update active hold notes: advance holdProgress based on elapsed time.
     * Returns list of auto-completed holds so GameEngine can score them.
     */
    fun updateHolds(currentTimeMs: Long): List<Pair<HitResult, ActiveNote>> {
        synchronized(lock) {
            val completed = mutableListOf<Pair<HitResult, ActiveNote>>()
            for (note in activeNotes) {
                if (!note.note.isHold || !note.isActive || note.isHit) continue
                if (note.holdStartTimeMs <= 0L) continue

                val durationMs = note.note.durationMs.toLong()
                if (durationMs <= 0L) continue

                val elapsed = currentTimeMs - note.holdStartTimeMs
                note.holdProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)

                // Auto-complete if held to full duration
                if (note.holdProgress >= 1f) {
                    note.isActive = false
                    note.isHit = true
                    note.hitResult = HitResult.PERFECT
                    completed.add(Pair(HitResult.PERFECT, note))
                }
            }
            return completed
        }
    }

    /**
     * Allocation-free hit detection: manual loop instead of filter+sort.
     */
    fun tryHit(lane: Int, currentTimeMs: Long): Pair<HitResult, ActiveNote?>? {
        synchronized(lock) {
            var bestTarget: ActiveNote? = null
            var bestDiff = Long.MAX_VALUE

            for (note in activeNotes) {
                if (note.isHit || note.note.lane != lane) continue
                // Skip hold notes already being tracked by the player
                if (note.note.isHold && note.isActive) continue
                val diff = kotlin.math.abs(note.note.timeMs - currentTimeMs)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestTarget = note
                }
            }

            val target = bestTarget ?: return null

            val result = when {
                bestDiff <= PERFECT_WINDOW_MS -> HitResult.PERFECT
                bestDiff <= GREAT_WINDOW_MS -> HitResult.GREAT
                bestDiff <= GOOD_WINDOW_MS -> HitResult.GOOD
                else -> return null
            }

            if (target.note.isHold) {
                // Hold note: start tracking hold, don't mark as hit yet
                target.isActive = true
                target.holdProgress = 0f
                target.holdStartTimeMs = currentTimeMs
                return Pair(result, target)
            }

            // Tap note: mark as hit immediately
            target.isHit = true
            target.hitResult = result

            return Pair(result, target)
        }
    }

    /**
     * Release a held note on the given lane.
     * Returns the hit result based on how much of the hold was completed.
     */
    fun releaseHold(lane: Int): Pair<HitResult, ActiveNote?>? {
        synchronized(lock) {
            for (note in activeNotes) {
                if (note.note.lane == lane && note.note.isHold && note.isActive && !note.isHit) {
                    note.isActive = false
                    note.isHit = true

                    val result = when {
                        note.holdProgress >= 0.80f -> HitResult.PERFECT
                        note.holdProgress >= 0.50f -> HitResult.GREAT
                        note.holdProgress >= 0.25f -> HitResult.GOOD
                        else -> HitResult.MISS
                    }
                    note.hitResult = result
                    return Pair(result, note)
                }
            }
            return null
        }
    }

    /**
     * Finalize all active hold notes — score them based on current progress.
     * Called when the game ends or pauses so active holds aren't silently dropped.
     */
    fun finalizeActiveHolds(): List<Pair<HitResult, ActiveNote>> {
        synchronized(lock) {
            val results = mutableListOf<Pair<HitResult, ActiveNote>>()
            for (note in activeNotes) {
                if (note.note.isHold && note.isActive && !note.isHit) {
                    note.isActive = false
                    note.isHit = true
                    val result = when {
                        note.holdProgress >= 0.80f -> HitResult.PERFECT
                        note.holdProgress >= 0.50f -> HitResult.GREAT
                        note.holdProgress >= 0.25f -> HitResult.GOOD
                        else -> HitResult.MISS
                    }
                    note.hitResult = result
                    results.add(Pair(result, note))
                }
            }
            return results
        }
    }

    fun getTotalNotes(): Int = notes.size

    fun reset() {
        synchronized(lock) {
            nextNoteIndex = 0
            activeNotes.clear()
        }
    }
}
