import math
from typing import List, Tuple

import numpy as np
from app.models.chart import Note, NoteType

# Guitar-focused frequency boundaries for 5 lanes (logarithmic scale)
# Covers standard guitar range: E2 (82 Hz) to E6 (~1318 Hz)
#
# Lane 1 (Green):  ~80-130 Hz   - Low E/A strings open & low frets
# Lane 2 (Red):    ~130-220 Hz  - A/D strings mid-range
# Lane 3 (Yellow): ~220-370 Hz  - D/G strings
# Lane 4 (Blue):   ~370-620 Hz  - G/B strings, higher frets
# Lane 5 (Orange): ~620-1400 Hz - B/high E strings, high frets & bends
LANE_BOUNDARIES = [80.0, 130.0, 220.0, 370.0, 620.0, 1400.0]

# Logarithmic boundaries for mapping
LOG_BOUNDARIES = [math.log2(f) for f in LANE_BOUNDARIES]


def frequency_to_lane(freq: float) -> int:
    """
    Map a guitar frequency to a lane (1-5) using logarithmic scale.
    Optimized for guitar range: low strings -> lane 1, high strings -> lane 5.
    """
    if freq <= 0:
        return 3  # Default to middle lane for unknown frequency

    log_freq = math.log2(freq)

    if log_freq <= LOG_BOUNDARIES[0]:
        return 1
    if log_freq >= LOG_BOUNDARIES[-1]:
        return 5

    for i in range(len(LOG_BOUNDARIES) - 1):
        if LOG_BOUNDARIES[i] <= log_freq < LOG_BOUNDARIES[i + 1]:
            return i + 1

    return 3


def centroids_to_lanes(centroids: list[float]) -> list[int]:
    """
    Map spectral centroid values to lanes 1-5 using percentile-based
    distribution. This adapts to each song's frequency characteristics
    rather than using fixed frequency boundaries.
    """
    if not centroids:
        return []

    arr = np.array(centroids)
    # Compute percentile boundaries: 20th, 40th, 60th, 80th
    p20, p40, p60, p80 = np.percentile(arr, [20, 40, 60, 80])

    lanes = []
    for c in centroids:
        if c <= p20:
            lanes.append(1)
        elif c <= p40:
            lanes.append(2)
        elif c <= p60:
            lanes.append(3)
        elif c <= p80:
            lanes.append(4)
        else:
            lanes.append(5)
    return lanes


def estimate_note_duration(
    onset_idx: int,
    onset_times: list[float],
    beat_times: list[float],
    min_duration_ms: int = 100,
) -> int:
    """
    Estimate note duration based on gap to next onset and beat spacing.
    Returns duration in milliseconds.
    """
    if onset_idx < len(onset_times) - 1:
        gap_ms = int((onset_times[onset_idx + 1] - onset_times[onset_idx]) * 1000)
        # Note duration is at most 80% of gap to next note
        duration = min(gap_ms * 80 // 100, 2000)
    else:
        # Last note - use beat spacing for duration
        if len(beat_times) >= 2:
            avg_beat_gap = (beat_times[-1] - beat_times[0]) / (len(beat_times) - 1)
            duration = int(avg_beat_gap * 500)  # Half a beat
        else:
            duration = 300

    return max(duration, min_duration_ms)


def create_notes(
    onset_times: list[float],
    frequencies: list[float],
    onset_strengths: list[float],
    beat_times: list[float],
) -> List[Note]:
    """
    Create Note objects from analysis results.
    Maps frequencies to lanes and determines note types (tap/hold).
    """
    notes = []
    beat_list = list(beat_times) if hasattr(beat_times, "tolist") else beat_times

    for i, (time_sec, freq, strength) in enumerate(
        zip(onset_times, frequencies, onset_strengths)
    ):
        lane = frequency_to_lane(freq)
        duration_ms = estimate_note_duration(i, list(onset_times), beat_list)

        # Determine note type based on duration
        note_type = NoteType.HOLD if duration_ms > 300 else NoteType.TAP

        # For tap notes, set a minimal duration
        if note_type == NoteType.TAP:
            duration_ms = min(duration_ms, 200)

        time_ms = int(time_sec * 1000)

        notes.append(
            Note(
                time_ms=time_ms,
                lane=lane,
                duration_ms=duration_ms,
                type=note_type,
            )
        )

    return notes
