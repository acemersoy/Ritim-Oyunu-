import os
import uuid
from pathlib import Path

from app.config import settings

UPLOAD_DIR = settings.UPLOAD_DIR


def ensure_upload_dir():
    os.makedirs(UPLOAD_DIR, exist_ok=True)


def generate_song_id() -> str:
    return str(uuid.uuid4())


def get_upload_path(song_id: str, filename: str) -> str:
    ensure_upload_dir()
    ext = Path(filename).suffix
    return os.path.join(UPLOAD_DIR, f"{song_id}{ext}")


def get_wav_path(song_id: str) -> str:
    ensure_upload_dir()
    return os.path.join(UPLOAD_DIR, f"{song_id}.wav")


def cleanup_files(song_id: str):
    for ext in [".mp3", ".wav", ".ogg", ".flac", ".m4a", ".aac"]:
        path = os.path.join(UPLOAD_DIR, f"{song_id}{ext}")
        if os.path.exists(path):
            os.remove(path)
