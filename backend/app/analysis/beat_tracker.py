import librosa
import numpy as np


def detect_tempo_and_beats(y: np.ndarray, sr: int) -> tuple[float, np.ndarray]:
    """
    Detect tempo (BPM) and beat positions.
    Returns (bpm, beat_times_seconds).
    """
    tempo, beat_frames = librosa.beat.beat_track(y=y, sr=sr)
    beat_times = librosa.frames_to_time(beat_frames, sr=sr)

    # tempo can be an array in some librosa versions
    if hasattr(tempo, "__len__"):
        bpm = float(tempo[0]) if len(tempo) > 0 else 120.0
    else:
        bpm = float(tempo)

    return bpm, beat_times


def get_downbeats(beat_times: np.ndarray, bpm: float) -> np.ndarray:
    """
    Estimate downbeats (strong beats, typically every 4 beats in 4/4 time).
    Returns array of downbeat times.
    """
    if len(beat_times) < 4:
        return beat_times

    # Assume 4/4 time signature - every 4th beat is a downbeat
    downbeats = beat_times[::4]
    return downbeats


def snap_to_beats(
    onset_times: np.ndarray,
    beat_times: np.ndarray,
    subdivision: int = 4,
) -> tuple[np.ndarray, np.ndarray]:
    """
    Quantize ALL onset times to a sub-beat grid.

    Generates a grid at 1/subdivision resolution between each beat pair,
    then snaps every onset to the nearest grid point. This ensures all
    notes are musically aligned (like Guitar Flash / Guitar Hero).

    subdivision=4 means sixteenth-note grid (4 divisions per beat).

    Returns (snapped_times, keep_indices) where keep_indices maps back
    to which original onsets survived deduplication.
    """
    if len(beat_times) < 2 or len(onset_times) == 0:
        return onset_times, np.arange(len(onset_times))

    # Build the sub-beat grid between each beat pair
    grid = []
    for i in range(len(beat_times) - 1):
        beat_start = beat_times[i]
        beat_end = beat_times[i + 1]
        sub_duration = (beat_end - beat_start) / subdivision
        for s in range(subdivision):
            grid.append(beat_start + s * sub_duration)
    # Add the last beat
    grid.append(beat_times[-1])

    # Extend grid before the first beat and after the last beat
    avg_beat_gap = float(np.mean(np.diff(beat_times)))
    sub_dur = avg_beat_gap / subdivision

    # Extend backwards
    t = beat_times[0] - sub_dur
    while t >= 0:
        grid.append(t)
        t -= sub_dur

    # Extend forwards
    max_time = float(onset_times[-1]) + 1.0
    t = beat_times[-1] + sub_dur
    while t <= max_time:
        grid.append(t)
        t += sub_dur

    grid = np.array(sorted(set(grid)))

    # Snap each onset to nearest grid point
    snapped = np.empty_like(onset_times)
    for i, onset in enumerate(onset_times):
        nearest_idx = np.argmin(np.abs(grid - onset))
        snapped[i] = grid[nearest_idx]

    # Remove duplicates: when two onsets snap to the same grid point, keep first
    seen = {}
    keep_indices = []
    for i, t in enumerate(snapped):
        key = round(t, 6)  # Avoid floating point issues
        if key not in seen:
            seen[key] = i
            keep_indices.append(i)

    keep_indices = np.array(keep_indices)
    return snapped[keep_indices], keep_indices


def classify_beat_positions(
    onset_times: np.ndarray,
    beat_times: np.ndarray,
) -> list[str]:
    """
    Classify each onset time by its position relative to beat grid.

    Returns list of position labels:
    - "ON_BEAT": falls on a quarter-note beat
    - "HALF_BEAT": falls on an eighth-note position (halfway between beats)
    - "QUARTER_BEAT": falls on a sixteenth-note position (other sub-beats)
    """
    if len(beat_times) < 2:
        return ["ON_BEAT"] * len(onset_times)

    avg_beat_gap = float(np.mean(np.diff(beat_times)))
    # Tolerance for matching: 5% of a beat gap
    tol = avg_beat_gap * 0.05

    positions = []
    for t in onset_times:
        # Find nearest beat
        diffs = np.abs(beat_times - t)
        min_diff = diffs.min()

        if min_diff < tol:
            positions.append("ON_BEAT")
        else:
            # Check if it's at a half-beat position
            half_beat_times = []
            for i in range(len(beat_times) - 1):
                half_beat_times.append((beat_times[i] + beat_times[i + 1]) / 2)
            if half_beat_times:
                half_diffs = np.abs(np.array(half_beat_times) - t)
                if half_diffs.min() < tol:
                    positions.append("HALF_BEAT")
                else:
                    positions.append("QUARTER_BEAT")
            else:
                positions.append("QUARTER_BEAT")

    return positions
