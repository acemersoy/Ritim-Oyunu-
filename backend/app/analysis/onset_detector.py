import librosa
import numpy as np


def detect_onsets(y: np.ndarray, sr: int) -> np.ndarray:
    """
    Detect note onset times in the audio signal.
    Returns array of onset times in seconds.
    """
    onset_env = librosa.onset.onset_strength(y=y, sr=sr)
    onset_frames = librosa.onset.onset_detect(
        y=y, sr=sr, onset_envelope=onset_env, backtrack=True
    )
    onset_times = librosa.frames_to_time(onset_frames, sr=sr)
    return onset_times


def detect_onsets_with_strength(y: np.ndarray, sr: int) -> list[tuple[float, float]]:
    """
    Detect onsets with their relative strength.
    Returns list of (time_seconds, strength) tuples.
    Strength is normalized to 0.0-1.0 range.
    """
    onset_env = librosa.onset.onset_strength(y=y, sr=sr)
    onset_frames = librosa.onset.onset_detect(
        y=y, sr=sr, onset_envelope=onset_env, backtrack=True
    )
    onset_times = librosa.frames_to_time(onset_frames, sr=sr)

    # Get strength at each onset frame
    strengths = onset_env[onset_frames]
    if len(strengths) > 0 and strengths.max() > 0:
        strengths = strengths / strengths.max()
    else:
        strengths = np.ones_like(onset_times)

    return list(zip(onset_times.tolist(), strengths.tolist()))
