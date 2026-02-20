"""
Audio analysis pipeline for on-device chart generation.
Adapted from backend pipeline.py for Chaquopy (Android).
"""
import json
import os

# Disable numba JIT (not available on Android)
os.environ["NUMBA_DISABLE_JIT"] = "1"

import numpy as np
import librosa

from beat_tracker import detect_tempo_and_beats
from difficulty import filter_by_difficulty
from lane_mapper import centroids_to_lanes
from chart import Chart, Difficulty, Note, NoteType


def analyze(wav_path, song_id="local", filename="unknown"):
    """
    Analyze a WAV file and return JSON string with charts for all difficulties.
    Called from Kotlin via Chaquopy.
    """
    try:
        y, sr = librosa.load(wav_path, sr=22050, mono=True)
        duration_ms = int(librosa.get_duration(y=y, sr=sr) * 1000)

        bpm, beat_times = detect_tempo_and_beats(y, sr)
        if len(beat_times) < 2:
            return json.dumps(_empty_result(song_id, filename, duration_ms, bpm))

        grid_times, grid_positions = _build_beat_grid(beat_times)

        onset_env = librosa.onset.onset_strength(y=y, sr=sr)
        onset_sr = sr / 512

        grid_strengths = _sample_strength_at_grid(grid_times, onset_env, onset_sr)

        if len(grid_strengths) > 0:
            threshold = np.percentile(grid_strengths, 40)
        else:
            return json.dumps(_empty_result(song_id, filename, duration_ms, bpm))

        active_mask = grid_strengths >= max(threshold, 0.01)
        active_times = grid_times[active_mask]
        active_positions = [p for p, m in zip(grid_positions, active_mask) if m]

        if len(active_times) == 0:
            return json.dumps(_empty_result(song_id, filename, duration_ms, bpm))

        centroids = _get_centroids_at_times(y, sr, active_times)
        lanes = centroids_to_lanes(centroids)

        base_notes = []
        for time_sec, lane in zip(active_times.tolist(), lanes):
            time_ms = int(time_sec * 1000)
            base_notes.append(Note(
                time_ms=time_ms,
                lane=lane,
                duration_ms=0,
                note_type=NoteType.TAP,
            ))

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

        result = {
            "song_id": song_id,
            "filename": filename,
            "duration_ms": duration_ms,
            "bpm": round(bpm, 1),
            "charts": charts,
        }
        return json.dumps(result)

    except Exception as e:
        return json.dumps({"error": str(e)})


def _build_beat_grid(beat_times):
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

    grid_times.append(beat_times[-1])
    grid_positions.append("ON_BEAT")

    return np.array(grid_times), grid_positions


def _sample_strength_at_grid(grid_times, onset_env, onset_sr):
    strengths = np.zeros(len(grid_times))
    window_frames = max(1, int(onset_sr * 0.03))

    for i, t in enumerate(grid_times):
        center_frame = int(t * onset_sr)
        start = max(0, center_frame - window_frames)
        end = min(len(onset_env), center_frame + window_frames + 1)
        if start < end:
            strengths[i] = onset_env[start:end].max()

    if strengths.max() > 0:
        strengths = strengths / strengths.max()

    return strengths


def _get_centroids_at_times(y, sr, times):
    centroids = librosa.feature.spectral_centroid(y=y, sr=sr)[0]
    frames = librosa.time_to_frames(times, sr=sr)
    result = []
    for f in frames:
        if f >= len(centroids):
            f = len(centroids) - 1
        result.append(float(centroids[f]))
    return result


def _empty_result(song_id, filename, duration_ms, bpm):
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
