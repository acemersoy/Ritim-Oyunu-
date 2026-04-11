import librosa
import numpy as np


def detect_tempo_and_beats(y: np.ndarray, sr: int) -> tuple[float, np.ndarray]:
    """
    Detect tempo (BPM) and beat positions using windowed beat tracking.

    For tracks longer than 90s, splits audio into overlapping 60s windows
    (15s overlap) so librosa.beat.beat_track() covers the full duration.
    Beats from adjacent windows are merged at the overlap midpoint.
    Gaps are filled with BPM-extrapolated beats.

    Returns (bpm, beat_times_seconds).
    """
    duration = len(y) / sr

    # Short tracks: single-pass detection
    if duration <= 90:
        return _detect_single_pass(y, sr)

    # --- Windowed detection for longer tracks ---
    window_sec = 60.0
    overlap_sec = 15.0
    step_sec = window_sec - overlap_sec  # 45s

    all_window_beats: list[tuple[float, float, np.ndarray]] = []
    all_bpms: list[float] = []

    start = 0.0
    while start < duration:
        end = min(start + window_sec, duration)

        # Skip very short final windows
        if end - start < 5.0:
            break

        start_sample = int(start * sr)
        end_sample = int(end * sr)
        y_window = y[start_sample:end_sample]

        tempo, beat_frames = librosa.beat.beat_track(y=y_window, sr=sr)
        beat_times = librosa.frames_to_time(beat_frames, sr=sr)

        # Convert to absolute time
        beat_times_abs = beat_times + start

        # Extract BPM
        if hasattr(tempo, "__len__"):
            bpm = float(tempo[0]) if len(tempo) > 0 else 0.0
        else:
            bpm = float(tempo)

        if bpm > 0:
            all_bpms.append(bpm)

        all_window_beats.append((start, end, beat_times_abs))
        start += step_sec

    if not all_bpms:
        return 120.0, np.array([])

    # Global BPM: median of all window BPMs
    global_bpm = float(np.median(all_bpms))

    # Merge beats using midpoint cutoff at each overlap
    merged_beats: list[float] = []
    n_windows = len(all_window_beats)

    for i, (w_start, w_end, beats) in enumerate(all_window_beats):
        if len(beats) == 0:
            continue

        if i == 0:
            # First window: keep beats below the midpoint of overlap with next
            if n_windows > 1:
                next_start = all_window_beats[1][0]
                cutoff = (next_start + w_end) / 2.0
                keep = beats[beats < cutoff]
            else:
                keep = beats
        elif i == n_windows - 1:
            # Last window: keep beats above the midpoint of overlap with previous
            prev_end = all_window_beats[i - 1][1]
            cutoff = (w_start + prev_end) / 2.0
            keep = beats[beats >= cutoff]
        else:
            # Middle window: keep beats between two midpoints
            prev_end = all_window_beats[i - 1][1]
            lower = (w_start + prev_end) / 2.0
            next_start = all_window_beats[i + 1][0]
            upper = (next_start + w_end) / 2.0
            keep = beats[(beats >= lower) & (beats < upper)]

        merged_beats.extend(keep.tolist())

    if len(merged_beats) < 2:
        # Fall back to BPM-based grid
        beat_period = 60.0 / global_bpm
        return global_bpm, np.arange(beat_period, duration, beat_period)

    merged_beats.sort()

    # Fill gaps where consecutive beats are too far apart (> 2x expected)
    beat_period = 60.0 / global_bpm
    gap_threshold = beat_period * 2.0

    filled: list[float] = [merged_beats[0]]
    for i in range(1, len(merged_beats)):
        gap = merged_beats[i] - filled[-1]
        if gap > gap_threshold:
            t = filled[-1] + beat_period
            while t < merged_beats[i] - beat_period * 0.5:
                filled.append(t)
                t += beat_period
        filled.append(merged_beats[i])

    # Extend to end of song if beats stop early
    last_beat = filled[-1]
    while last_beat + beat_period < duration - 1.0:
        last_beat += beat_period
        filled.append(last_beat)

    return global_bpm, np.array(filled)


def _detect_single_pass(y: np.ndarray, sr: int) -> tuple[float, np.ndarray]:
    """Original single-pass beat detection for short tracks."""
    tempo, beat_frames = librosa.beat.beat_track(y=y, sr=sr)
    beat_times = librosa.frames_to_time(beat_frames, sr=sr)

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
