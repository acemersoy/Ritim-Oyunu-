from pydantic import BaseModel
from typing import List, Optional
from enum import Enum


class DifficultyEnum(str, Enum):
    EASY = "easy"
    MEDIUM = "medium"
    HARD = "hard"


class NoteResponse(BaseModel):
    time_ms: int
    lane: int
    duration_ms: int
    type: str


class ChartResponse(BaseModel):
    song_id: str
    title: str
    duration_ms: int
    bpm: float
    difficulty: str
    notes: List[NoteResponse]


class UploadResponse(BaseModel):
    song_id: str
    status: str
    message: str


class StatusResponse(BaseModel):
    song_id: str
    status: str
    message: Optional[str] = None


class ShareRequest(BaseModel):
    song_id: str


class ShareResponse(BaseModel):
    share_url: str
    share_token: str


class SongListItem(BaseModel):
    song_id: str
    filename: str
    status: str
    bpm: Optional[float] = None
    duration_ms: Optional[int] = None


class DeleteResponse(BaseModel):
    message: str


class SyncScoreRequest(BaseModel):
    user_id: str
    username: str
    song_id: str
    score: int
    accuracy: float
    max_combo: int
    difficulty: str
    xp_earned: int


class ProfileResponse(BaseModel):
    user_id: str
    username: str
    level: int
    total_xp: int
    league: str
    highest_score: int
    rank: Optional[int] = None


class LeaderboardEntry(BaseModel):
    user_id: str
    username: str
    level: int
    league: str
    highest_score: int
    rank: int
