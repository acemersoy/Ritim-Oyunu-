import os
import librosa
import numpy as np
from pydub import AudioSegment


def convert_to_wav(input_path: str, wav_path: str) -> str:
    """Convert any supported audio format to WAV for librosa processing.
    Uses ffmpeg auto-detection instead of relying on file extension."""
    # Let pydub/ffmpeg auto-detect the format (handles mismatched extensions)
    audio = AudioSegment.from_file(input_path)
    audio.export(wav_path, format="wav")
    return wav_path


def load_audio(wav_path: str, sr: int = 22050) -> tuple[np.ndarray, int]:
    """Load audio file using librosa. Returns (audio_signal, sample_rate)."""
    y, sr_out = librosa.load(wav_path, sr=sr, mono=True)
    return y, sr_out


def get_duration_ms(y: np.ndarray, sr: int) -> int:
    """Get audio duration in milliseconds."""
    return int(librosa.get_duration(y=y, sr=sr) * 1000)
