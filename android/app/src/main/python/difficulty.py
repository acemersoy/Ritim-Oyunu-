from chart import Note, NoteType, Difficulty


def filter_by_difficulty(notes, difficulty, beat_positions):
    if not notes:
        return []

    if difficulty == Difficulty.EASY:
        return _filter_easy(notes, beat_positions)
    elif difficulty == Difficulty.MEDIUM:
        return _filter_medium(notes, beat_positions)
    else:
        return _filter_hard(notes, beat_positions)


def _filter_easy(notes, positions):
    easy_lanes = [1, 3, 5]
    result = []
    for note, pos in zip(notes, positions):
        if pos != "ON_BEAT":
            continue
        assigned_lane = easy_lanes[note.lane % 3]
        result.append(Note(
            time_ms=note.time_ms,
            lane=assigned_lane,
            duration_ms=0,
            note_type=NoteType.TAP,
        ))
    return result


def _filter_medium(notes, positions):
    result = []
    for note, pos in zip(notes, positions):
        if pos == "QUARTER_BEAT":
            continue
        result.append(Note(
            time_ms=note.time_ms,
            lane=note.lane,
            duration_ms=0,
            note_type=NoteType.TAP,
        ))
    return result


def _filter_hard(notes, positions):
    result = []
    for i, (note, pos) in enumerate(zip(notes, positions)):
        if pos == "ON_BEAT" and i < len(notes) - 1:
            gap_ms = notes[i + 1].time_ms - note.time_ms
            if gap_ms > 400:
                hold_duration = min(gap_ms - 100, 1500)
                result.append(Note(
                    time_ms=note.time_ms,
                    lane=note.lane,
                    duration_ms=hold_duration,
                    note_type=NoteType.HOLD,
                ))
                continue

        result.append(Note(
            time_ms=note.time_ms,
            lane=note.lane,
            duration_ms=0,
            note_type=NoteType.TAP,
        ))
    return result
