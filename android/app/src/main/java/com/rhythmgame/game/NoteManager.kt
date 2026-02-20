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

    // Hit windows in milliseconds
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
                // Keep updating y for all notes, even if hit or missed, 
                // so they can eventually fall off-screen and be removed.
                val noteTimeMs = activeNote.note.timeMs
                val progress = (currentTimeMs - (noteTimeMs - approachTimeMs)).toFloat() / approachTimeMs.toFloat()
                activeNote.y = progress * hitLineY

                // Mark missed notes: only if not already hit or marked as miss
                if (!activeNote.isHit && activeNote.hitResult == null && currentTimeMs > noteTimeMs + MISS_THRESHOLD_MS) {
                    activeNote.isHit = true
                    activeNote.hitResult = HitResult.MISS
                }
            }

            // Capture the state BEFORE removal for the engine to process
            val resultList = activeNotes.toList()

            // Remove notes that have scrolled past and been resolved
            activeNotes.removeAll { note ->
                val isResolved = note.isHit || note.hitResult == HitResult.MISS
                isResolved && note.y > hitLineY + 800f
            }

            return resultList
        }
    }

    fun tryHit(lane: Int, currentTimeMs: Long): Pair<HitResult, ActiveNote?>? {
        synchronized(lock) {
            val candidates = activeNotes
                .filter { !it.isHit && it.note.lane == lane }
                .sortedBy { kotlin.math.abs(it.note.timeMs - currentTimeMs) }

            val target = candidates.firstOrNull() ?: return null
            val diff = kotlin.math.abs(target.note.timeMs - currentTimeMs)

            val result = when {
                diff <= PERFECT_WINDOW_MS -> HitResult.PERFECT
                diff <= GREAT_WINDOW_MS -> HitResult.GREAT
                diff <= GOOD_WINDOW_MS -> HitResult.GOOD
                else -> return null
            }

            target.isHit = true
            target.hitResult = result

            return Pair(result, target)
        }
    }

    fun releaseHold(lane: Int) {
        synchronized(lock) {
            activeNotes
                .filter { it.note.lane == lane && it.note.isHold && it.isActive }
                .forEach { it.isActive = false }
        }
    }

    fun getActiveNotes(): List<ActiveNote> = synchronized(lock) { activeNotes.toList() }

    fun getTotalNotes(): Int = notes.size

    fun allNotesProcessed(currentTimeMs: Long): Boolean {
        synchronized(lock) {
            return nextNoteIndex >= notes.size &&
                activeNotes.all { it.isHit || it.hitResult == HitResult.MISS }
        }
    }

    fun reset() {
        synchronized(lock) {
            nextNoteIndex = 0
            activeNotes.clear()
        }
    }
}
