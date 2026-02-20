from dataclasses import dataclass, field
from enum import Enum
from typing import List, Optional


class NoteType(str, Enum):
    TAP = "tap"
    HOLD = "hold"


class Difficulty(str, Enum):
    EASY = "easy"
    MEDIUM = "medium"
    HARD = "hard"


@dataclass
class Note:
    time_ms: int
    lane: int  # 1-5
    duration_ms: int = 0
    type: NoteType = NoteType.TAP

    def to_dict(self) -> dict:
        return {
            "time_ms": self.time_ms,
            "lane": self.lane,
            "duration_ms": self.duration_ms,
            "type": self.type.value,
        }


@dataclass
class Chart:
    song_id: str
    title: str
    duration_ms: int
    bpm: float
    difficulty: Difficulty
    notes: List[Note] = field(default_factory=list)

    def to_dict(self) -> dict:
        return {
            "song_id": self.song_id,
            "title": self.title,
            "duration_ms": self.duration_ms,
            "bpm": self.bpm,
            "difficulty": self.difficulty.value,
            "notes": [n.to_dict() for n in self.notes],
        }
