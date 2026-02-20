"""
Audio analysis pipeline: beat-grid-first approach.

Instead of detecting onsets and snapping to grid, this pipeline:
1. Builds a beat grid (quarter, eighth, sixteenth notes)
2. Computes onset strength at each grid point
3. Places notes where there's musical energy
4. Uses spectral centroid for lane assignment

This ensures notes always land on rhythmically correct positions
while still responding to actual musical events.
"""
import numpy as np
import librosa

from app.analysis.audio_loader import convert_to_wav, load_audio, get_duration_ms
from app.analysis.beat_tracker import detect_tempo_and_beats
from app.analysis.difficulty import filter_by_difficulty
from app.analysis.lane_mapper import centroids_to_lanes
from app.models.chart import Chart, Difficulty, Note, NoteType


def analyze_audio(
    input_path: str, wav_path: str, song_id: str, filename: str
) -> dict:
    """
    Run the beat-grid-first analysis pipeline.
    Returns dict with song metadata and charts for all difficulties.
    """
    # Step 1: Convert to WAV
    convert_to_wav(input_path, wav_path)

    # Step 2: Load audio (full mix)
    y, sr = load_audio(wav_path)
    duration_ms = get_duration_ms(y, sr)

    # Step 3: Beat tracking on full mix
    bpm, beat_times = detect_tempo_and_beats(y, sr)
    if len(beat_times) < 2:
        return _empty_result(song_id, filename, duration_ms, bpm)

    # Step 4: Build the full beat grid with position labels
    grid_times, grid_positions = _build_beat_grid(beat_times)

    # Step 5: Compute onset strength envelope
    onset_env = librosa.onset.onset_strength(y=y, sr=sr)
    onset_sr = sr / 512  # onset envelope sample rate (default hop_length=512)

    # Step 6: Sample onset strength at each grid point
    grid_strengths = _sample_strength_at_grid(grid_times, onset_env, onset_sr)

    # Step 7: Adaptive threshold - keep grid points with energy above median
    # Use a percentile-based threshold that adapts to the song
    if len(grid_strengths) > 0:
        threshold = np.percentile(grid_strengths, 40)  # keep top 60%
    else:
        return _empty_result(song_id, filename, duration_ms, bpm)

    # Step 8: Filter grid points by energy
    active_mask = grid_strengths >= max(threshold, 0.01)
    active_times = grid_times[active_mask]
    active_positions = [p for p, m in zip(grid_positions, active_mask) if m]
    active_strengths = grid_strengths[active_mask]

    if len(active_times) == 0:
        return _empty_result(song_id, filename, duration_ms, bpm)

    # Step 9: Spectral centroid at each active grid point for lane mapping
    centroids = _get_centroids_at_times(y, sr, active_times)
    lanes = centroids_to_lanes(centroids)

    # Step 10: Create base notes
    base_notes = []
    for time_sec, lane in zip(active_times.tolist(), lanes):
        time_ms = int(time_sec * 1000)
        base_notes.append(Note(
            time_ms=time_ms,
            lane=lane,
            duration_ms=0,
            type=NoteType.TAP,
        ))

    # Step 11: Generate charts for all difficulties
    charts = {}
    for diff in Difficulty:
        filtered_notes = filter_by_difficulty(base_notes, diff, active_positions)
        chart = Chart(
            song_id=song_id,
            title=filename,
            duration_ms=duration_ms,
            bpm=round(bpm, 1),
            difficulty=diff,
            notes=filtered_notes,
        )
        charts[diff.value] = chart.to_dict()

    return {
        "song_id": song_id,
        "filename": filename,
        "duration_ms": duration_ms,
        "bpm": round(bpm, 1),
        "charts": charts,
    }


def _build_beat_grid(
    beat_times: np.ndarray,
) -> tuple[np.ndarray, list[str]]:
    """
    Build a complete sub-beat grid from beat positions.
    Each grid point gets a position label: ON_BEAT, HALF_BEAT, QUARTER_BEAT.

    Grid has 4 subdivisions per beat (sixteenth-note resolution).
    """
    grid_times = []
    grid_positions = []

    for i in range(len(beat_times) - 1):
        beat_start = beat_times[i]
        beat_end = beat_times[i + 1]
        sub_dur = (beat_end - beat_start) / 4

        for s in range(4):
            t = beat_start + s * sub_dur
            grid_times.append(t)
            if s == 0:
                grid_positions.append("ON_BEAT")
            elif s == 2:
                grid_positions.append("HALF_BEAT")
            else:
                grid_positions.append("QUARTER_BEAT")

    # Add the last beat
    grid_times.append(beat_times[-1])
    grid_positions.append("ON_BEAT")

    return np.array(grid_times), grid_positions


def _sample_strength_at_grid(
    grid_times: np.ndarray,
    onset_env: np.ndarray,
    onset_sr: float,
) -> np.ndarray:
    """
    Sample the onset strength envelope at each grid time.
    Uses the maximum strength within a small window around each grid point.
    """
    strengths = np.zeros(len(grid_times))
    window_frames = max(1, int(onset_sr * 0.03))  # 30ms window

    for i, t in enumerate(grid_times):
        center_frame = int(t * onset_sr)
        start = max(0, center_frame - window_frames)
        end = min(len(onset_env), center_frame + window_frames + 1)
        if start < end:
            strengths[i] = onset_env[start:end].max()

    # Normalize to 0-1
    if strengths.max() > 0:
        strengths = strengths / strengths.max()

    return strengths


def _get_centroids_at_times(
    y: np.ndarray, sr: int, times: np.ndarray
) -> list[float]:
    """Get spectral centroid at specific time points."""
    centroids = librosa.feature.spectral_centroid(y=y, sr=sr)[0]
    frames = librosa.time_to_frames(times, sr=sr)
    result = []
    for f in frames:
        if f >= len(centroids):
            f = len(centroids) - 1
        result.append(float(centroids[f]))
    return result


def _empty_result(song_id: str, filename: str, duration_ms: int, bpm: float) -> dict:
    charts = {}
    for diff in Difficulty:
        chart = Chart(
            song_id=song_id,
            title=filename,
            duration_ms=duration_ms,
            bpm=round(bpm, 1),
            difficulty=diff,
            notes=[],
        )
        charts[diff.value] = chart.to_dict()

    return {
        "song_id": song_id,
        "filename": filename,
        "duration_ms": duration_ms,
        "bpm": round(bpm, 1),
        "charts": charts,
    }
