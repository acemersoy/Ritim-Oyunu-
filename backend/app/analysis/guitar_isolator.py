"""
Guitar isolation and guitar-focused audio analysis.

Isolates the guitar signal from a full mix using:
1. Harmonic/percussive source separation (HPSS) - removes drums
2. Band-pass filtering for guitar fundamental range (~80 Hz - 1200 Hz)
3. Spectral filtering to emphasize guitar timbral characteristics

Guitar frequency ranges:
- Standard tuning lowest note: E2 = 82.4 Hz
- Highest fret on high E: ~1318 Hz (E6)
- Harmonics extend up to ~5 kHz
- Most guitar energy sits between 80 Hz - 5 kHz
"""
import librosa
import numpy as np


# Guitar fundamental frequency range
GUITAR_FREQ_LOW = 80.0     # E2 = 82.4 Hz
GUITAR_FREQ_HIGH = 1400.0  # ~E6 top fret range
# Harmonics extend further
GUITAR_HARMONIC_HIGH = 5000.0


def isolate_guitar(y: np.ndarray, sr: int) -> np.ndarray:
    """
    Isolate guitar-like harmonic content from a full mix.

    Strategy:
    1. HPSS to separate harmonic from percussive (drums)
    2. Band-pass filter the harmonic part to guitar range
    3. This gives us the best approximation of guitar in a polyphonic mix
    """
    # Step 1: Harmonic/percussive separation - remove drums/percussion
    y_harmonic, y_percussive = librosa.effects.hpss(y, margin=3.0)

    # Step 2: Band-pass filter for guitar fundamental + harmonics range
    y_guitar = _bandpass_filter(y_harmonic, sr, GUITAR_FREQ_LOW, GUITAR_HARMONIC_HIGH)

    return y_guitar


def _bandpass_filter(
    y: np.ndarray, sr: int, low_freq: float, high_freq: float
) -> np.ndarray:
    """
    Apply a band-pass filter using STFT domain masking.
    Keeps only frequencies between low_freq and high_freq.
    """
    # Compute STFT
    n_fft = 2048
    S = librosa.stft(y, n_fft=n_fft)

    # Create frequency mask
    freqs = librosa.fft_frequencies(sr=sr, n_fft=n_fft)
    mask = np.zeros_like(freqs)
    band = (freqs >= low_freq) & (freqs <= high_freq)
    mask[band] = 1.0

    # Smooth mask edges to avoid artifacts
    from scipy.ndimage import gaussian_filter1d
    mask = gaussian_filter1d(mask, sigma=2)

    # Apply mask
    S_filtered = S * mask[:, np.newaxis]

    # Inverse STFT
    y_filtered = librosa.istft(S_filtered, length=len(y))
    return y_filtered


def detect_guitar_onsets(y_guitar: np.ndarray, sr: int) -> list[tuple[float, float]]:
    """
    Detect onsets specifically in the guitar-isolated signal.
    Returns list of (time_seconds, strength) tuples.
    """
    onset_env = librosa.onset.onset_strength(y=y_guitar, sr=sr)
    onset_frames = librosa.onset.onset_detect(
        y=y_guitar, sr=sr, onset_envelope=onset_env, backtrack=True
    )
    onset_times = librosa.frames_to_time(onset_frames, sr=sr)

    strengths = onset_env[onset_frames]
    if len(strengths) > 0 and strengths.max() > 0:
        strengths = strengths / strengths.max()
    else:
        strengths = np.ones_like(onset_times)

    return list(zip(onset_times.tolist(), strengths.tolist()))


def detect_guitar_pitches(
    y_guitar: np.ndarray, sr: int, onset_times: np.ndarray
) -> list[float]:
    """
    Detect pitches in the guitar-isolated signal.
    More accurate than full-mix pitch detection since competing
    instruments are largely removed.
    """
    pitches, magnitudes = librosa.piptrack(
        y=y_guitar, sr=sr,
        fmin=GUITAR_FREQ_LOW,
        fmax=GUITAR_FREQ_HIGH,
    )

    onset_frames = librosa.time_to_frames(onset_times, sr=sr)
    frequencies = []

    for frame_idx in onset_frames:
        if frame_idx >= pitches.shape[1]:
            frame_idx = pitches.shape[1] - 1

        mag_slice = magnitudes[:, frame_idx]
        pitch_slice = pitches[:, frame_idx]

        # Only consider pitches in guitar range
        valid_mask = (pitch_slice >= GUITAR_FREQ_LOW) & (pitch_slice <= GUITAR_FREQ_HIGH)
        if valid_mask.any():
            valid_pitches = pitch_slice[valid_mask]
            valid_mags = mag_slice[valid_mask]
            best_idx = valid_mags.argmax()
            frequencies.append(float(valid_pitches[best_idx]))
        else:
            # Fallback: spectral centroid of guitar signal at this frame
            centroid = librosa.feature.spectral_centroid(y=y_guitar, sr=sr)
            if frame_idx < centroid.shape[1]:
                frequencies.append(float(centroid[0, frame_idx]))
            else:
                frequencies.append(440.0)  # A4 default

    return frequencies


def estimate_guitar_note_sustain(
    y_guitar: np.ndarray,
    sr: int,
    onset_time: float,
    next_onset_time: float | None,
    min_sustain_ms: int = 100,
    max_sustain_ms: int = 3000,
) -> int:
    """
    Estimate how long a guitar note sustains by analyzing the
    amplitude envelope after the onset.

    Guitar notes have a characteristic attack-sustain-decay envelope.
    We find where the amplitude drops below a threshold to determine
    note end.
    """
    onset_sample = int(onset_time * sr)
    max_samples = int(max_sustain_ms / 1000.0 * sr)

    if next_onset_time is not None:
        # Don't extend past the next onset
        next_sample = int(next_onset_time * sr)
        max_samples = min(max_samples, next_sample - onset_sample)

    end_sample = min(onset_sample + max_samples, len(y_guitar))
    if onset_sample >= end_sample:
        return min_sustain_ms

    segment = np.abs(y_guitar[onset_sample:end_sample])
    if len(segment) == 0:
        return min_sustain_ms

    # Compute RMS envelope in short frames
    frame_length = int(0.02 * sr)  # 20ms frames
    if frame_length == 0:
        return min_sustain_ms

    n_frames = len(segment) // frame_length
    if n_frames == 0:
        return min_sustain_ms

    rms = np.array([
        np.sqrt(np.mean(segment[i * frame_length:(i + 1) * frame_length] ** 2))
        for i in range(n_frames)
    ])

    if len(rms) == 0 or rms[0] == 0:
        return min_sustain_ms

    # Find where RMS drops below 15% of peak
    peak_rms = rms.max()
    threshold = peak_rms * 0.15
    below_threshold = np.where(rms < threshold)[0]

    if len(below_threshold) > 0:
        sustain_frames = below_threshold[0]
    else:
        sustain_frames = n_frames

    sustain_ms = int(sustain_frames * frame_length / sr * 1000)
    return max(min_sustain_ms, min(sustain_ms, max_sustain_ms))
