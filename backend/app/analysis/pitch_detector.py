import librosa
import numpy as np


def detect_pitches_at_onsets(
    y: np.ndarray, sr: int, onset_times: np.ndarray
) -> list[float]:
    """
    Detect dominant pitch (frequency in Hz) at each onset time.
    Uses piptrack for polyphonic pitch detection.
    Returns list of frequencies (Hz) corresponding to each onset.
    """
    # Compute pitch track using piptrack
    pitches, magnitudes = librosa.piptrack(y=y, sr=sr)

    onset_frames = librosa.time_to_frames(onset_times, sr=sr)
    frequencies = []

    for frame_idx in onset_frames:
        if frame_idx >= pitches.shape[1]:
            frame_idx = pitches.shape[1] - 1

        # Get the pitch bin with highest magnitude at this frame
        mag_slice = magnitudes[:, frame_idx]
        pitch_slice = pitches[:, frame_idx]

        # Filter out zero pitches
        valid_mask = pitch_slice > 0
        if valid_mask.any():
            valid_pitches = pitch_slice[valid_mask]
            valid_mags = mag_slice[valid_mask]
            best_idx = valid_mags.argmax()
            frequencies.append(float(valid_pitches[best_idx]))
        else:
            # No clear pitch detected - use spectral centroid as fallback
            frequencies.append(0.0)

    return frequencies


def get_spectral_centroid_at_onsets(
    y: np.ndarray, sr: int, onset_times: np.ndarray
) -> list[float]:
    """
    Get spectral centroid at each onset time as a fallback frequency measure.
    Useful when pitch detection fails on percussive/noisy audio.
    """
    centroids = librosa.feature.spectral_centroid(y=y, sr=sr)[0]
    onset_frames = librosa.time_to_frames(onset_times, sr=sr)
    result = []

    for frame_idx in onset_frames:
        if frame_idx >= len(centroids):
            frame_idx = len(centroids) - 1
        result.append(float(centroids[frame_idx]))

    return result
