package com.rhythmgame.game

import com.rhythmgame.data.model.Note

class NoteManager(
    private val notes: List<Note>,
    private val hitLineY: Float,
    private val screenHeight: Float,
    private val approachTimeMs: Long = 2000L,
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

                // Mark missed notes
                if (!activeNote.isHit && activeNote.hitResult == null && currentTimeMs > noteTimeMs + MISS_THRESHOLD_MS) {
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
     * Allocation-free hit detection: manual loop instead of filter+sort.
     */
    fun tryHit(lane: Int, currentTimeMs: Long): Pair<HitResult, ActiveNote?>? {
        synchronized(lock) {
            var bestTarget: ActiveNote? = null
            var bestDiff = Long.MAX_VALUE

            for (note in activeNotes) {
                if (note.isHit || note.note.lane != lane) continue
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

            target.isHit = true
            target.hitResult = result

            return Pair(result, target)
        }
    }

    fun releaseHold(lane: Int) {
        synchronized(lock) {
            for (note in activeNotes) {
                if (note.note.lane == lane && note.note.isHold && note.isActive) {
                    note.isActive = false
                }
            }
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
