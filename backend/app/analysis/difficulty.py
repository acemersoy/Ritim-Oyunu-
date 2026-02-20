"""
Beat-position-aware difficulty filtering.

Notes are filtered based on their rhythmic position:
- Easy:   Only on-beat notes (quarter notes), 3 lanes
- Medium: On-beat + half-beat notes (eighth notes), 5 lanes
- Hard:   All notes (sixteenth notes), 5 lanes, holds on strong beats
"""
from typing import List

from app.models.chart import Note, NoteType, Difficulty


def filter_by_difficulty(
    notes: List[Note],
    difficulty: Difficulty,
    beat_positions: list[str],
) -> List[Note]:
    """
    Filter notes based on difficulty using their beat position classification.
    beat_positions: list of "ON_BEAT", "HALF_BEAT", "QUARTER_BEAT" per note.
    """
    if not notes:
        return []

    if difficulty == Difficulty.EASY:
        return _filter_easy(notes, beat_positions)
    elif difficulty == Difficulty.MEDIUM:
        return _filter_medium(notes, beat_positions)
    else:
        return _filter_hard(notes, beat_positions)


def _filter_easy(notes: List[Note], positions: list[str]) -> List[Note]:
    """
    Easy: Only on-beat notes, cycle through 3 lanes (1, 3, 5), all TAP.
    Uses cycling to ensure balanced distribution across lanes.
    """
    easy_lanes = [1, 3, 5]
    result = []
    lane_idx = 0
    for note, pos in zip(notes, positions):
        if pos != "ON_BEAT":
            continue
        # Cycle through easy lanes based on the note's original lane
        # This keeps some variation while staying balanced
        assigned_lane = easy_lanes[note.lane % 3]
        result.append(Note(
            time_ms=note.time_ms,
            lane=assigned_lane,
            duration_ms=0,
            type=NoteType.TAP,
        ))
        lane_idx += 1
    return result


def _filter_medium(notes: List[Note], positions: list[str]) -> List[Note]:
    """
    Medium: On-beat + half-beat notes, 5 lanes, all TAP.
    """
    result = []
    for note, pos in zip(notes, positions):
        if pos == "QUARTER_BEAT":
            continue
        result.append(Note(
            time_ms=note.time_ms,
            lane=note.lane,
            duration_ms=0,
            type=NoteType.TAP,
        ))
    return result


def _filter_hard(notes: List[Note], positions: list[str]) -> List[Note]:
    """
    Hard: All notes. On-beat notes with enough gap become HOLDs.
    """
    result = []
    for i, (note, pos) in enumerate(zip(notes, positions)):
        # On-beat notes with gap > 400ms become HOLDs
        if pos == "ON_BEAT" and i < len(notes) - 1:
            gap_ms = notes[i + 1].time_ms - note.time_ms
            if gap_ms > 400:
                hold_duration = min(gap_ms - 100, 1500)
                result.append(Note(
                    time_ms=note.time_ms,
                    lane=note.lane,
                    duration_ms=hold_duration,
                    type=NoteType.HOLD,
                ))
                continue

        result.append(Note(
            time_ms=note.time_ms,
            lane=note.lane,
            duration_ms=0,
            type=NoteType.TAP,
        ))
    return result
