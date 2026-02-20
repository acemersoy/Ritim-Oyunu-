from enum import Enum


class NoteType(str, Enum):
    TAP = "tap"
    HOLD = "hold"


class Difficulty(str, Enum):
    EASY = "easy"
    MEDIUM = "medium"
    HARD = "hard"


class Note:
    def __init__(self, time_ms, lane, duration_ms=0, note_type=NoteType.TAP):
        self.time_ms = time_ms
        self.lane = lane
        self.duration_ms = duration_ms
        self.type = note_type

    def to_dict(self):
        return {
            "time_ms": self.time_ms,
            "lane": self.lane,
            "duration_ms": self.duration_ms,
            "type": self.type.value,
        }


class Chart:
    def __init__(self, song_id, title, duration_ms, bpm, difficulty, notes=None):
        self.song_id = song_id
        self.title = title
        self.duration_ms = duration_ms
        self.bpm = bpm
        self.difficulty = difficulty
        self.notes = notes or []

    def to_dict(self):
        return {
            "song_id": self.song_id,
            "title": self.title,
            "duration_ms": self.duration_ms,
            "bpm": self.bpm,
            "difficulty": self.difficulty.value,
            "notes": [n.to_dict() for n in self.notes],
        }
