"""
Per-stem musical event analysis.

Drums:  Sub-band splitting → onset detection per band (kick/snare/hihat).
Bass:   Basic Pitch polyphonic note detection → MIDI note events.
Vocals: Basic Pitch note detection → pitch contour + onsets.
Other:  Basic Pitch note detection → guitar/synth/keys notes.
"""
import logging
import tempfile
import os
from dataclasses import dataclass
from typing import Optional

import librosa
import numpy as np
import soundfile as sf

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
#  Data classes for typed analysis results
# ---------------------------------------------------------------------------

@dataclass
class DrumEvent:
    time_sec: float
    drum_type: str  # 'kick', 'snare', 'hihat'
    strength: float  # 0-1 normalized


@dataclass
class PitchedEvent:
    time_sec: float
    duration_sec: float
    midi_pitch: int  # 0-127
    amplitude: float  # 0-1
    source: str  # 'bass', 'vocals', 'other'


# ---------------------------------------------------------------------------
#  Drum stem analysis
# ---------------------------------------------------------------------------

def analyze_drums(
    drum_stem: np.ndarray,
    sr: int,
    min_rms: float = 0.005,
) -> list[DrumEvent]:
    """
    Analyze the isolated drum stem by splitting into frequency sub-bands
    and detecting onsets in each.

    Sub-bands:
      kick:  20-150 Hz   (bass drum fundamentals)
      snare: 150-1000 Hz (snare body + crack)
      hihat: 3000+ Hz    (hi-hat, cymbals, ride)

    Returns sorted list of DrumEvents.
    """
    rms = np.sqrt(np.mean(drum_stem ** 2))
    if rms < min_rms:
        logger.info("Drum stem is near-silent, skipping drum analysis")
        return []

    S = librosa.stft(drum_stem, n_fft=2048)
    freqs = librosa.fft_frequencies(sr=sr, n_fft=2048)

    events: list[DrumEvent] = []

    bands = [
        ("kick",  20,   150),
        ("snare", 150,  1000),
        ("hihat", 3000, sr // 2),
    ]

    for drum_type, lo, hi in bands:
        mask = ((freqs >= lo) & (freqs <= hi)).astype(np.float32)
        S_band = S * mask[:, np.newaxis]
        y_band = librosa.istft(S_band, length=len(drum_stem))

        band_rms = np.sqrt(np.mean(y_band ** 2))
        if band_rms < min_rms * 0.5:
            continue

        # Onset detection
        onset_env = librosa.onset.onset_strength(y=y_band, sr=sr)
        onset_frames = librosa.onset.onset_detect(
            y=y_band, sr=sr, onset_envelope=onset_env, backtrack=False,
        )
        if len(onset_frames) == 0:
            continue

        onset_times = librosa.frames_to_time(onset_frames, sr=sr)

        # Normalized strengths
        strengths = onset_env[onset_frames]
        max_str = strengths.max()
        if max_str > 0:
            strengths = strengths / max_str

        for t, s in zip(onset_times.tolist(), strengths.tolist()):
            events.append(DrumEvent(time_sec=t, drum_type=drum_type, strength=s))

    events.sort(key=lambda e: e.time_sec)
    logger.info(f"Drum analysis: {len(events)} events "
                f"(kick={sum(1 for e in events if e.drum_type=='kick')}, "
                f"snare={sum(1 for e in events if e.drum_type=='snare')}, "
                f"hihat={sum(1 for e in events if e.drum_type=='hihat')})")
    return events


# ---------------------------------------------------------------------------
#  Pitched stem analysis (bass, vocals, other) via Basic Pitch
# ---------------------------------------------------------------------------

def analyze_pitched_stem(
    stem: np.ndarray,
    sr: int,
    source_name: str,
    min_rms: float = 0.005,
    onset_threshold: float = 0.5,
    frame_threshold: float = 0.3,
    minimum_note_length_ms: float = 80.0,
) -> list[PitchedEvent]:
    """
    Detect polyphonic note events in a pitched stem using Basic Pitch (Spotify).

    Basic Pitch uses a lightweight ONNX neural network for multi-pitch detection.
    It returns MIDI note events with onset, offset, pitch, and amplitude.

    Args:
        stem: Mono audio numpy array.
        sr: Sample rate.
        source_name: 'bass', 'vocals', or 'other'.
        onset_threshold: Sensitivity for note onsets (lower = more notes).
        frame_threshold: Sensitivity for note frames (lower = more notes).
        minimum_note_length_ms: Minimum detected note length in ms.

    Returns:
        Sorted list of PitchedEvents.
    """
    rms = np.sqrt(np.mean(stem ** 2))
    if rms < min_rms:
        logger.info(f"{source_name} stem is near-silent, skipping")
        return []

    try:
        from basic_pitch.inference import predict
        from basic_pitch import ICASSP_2022_MODEL_PATH
    except ImportError:
        logger.error("basic-pitch not installed, falling back to onset-only")
        return _onset_only_fallback(stem, sr, source_name)

    # Basic Pitch expects audio at 22050 Hz
    if sr != 22050:
        stem_22k = librosa.resample(stem, orig_sr=sr, target_sr=22050)
    else:
        stem_22k = stem

    # Save to temp file (Basic Pitch works most reliably with file paths)
    tmp_path = None
    try:
        with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
            tmp_path = tmp.name
            sf.write(tmp_path, stem_22k, 22050)

        logger.info(f"Running Basic Pitch on {source_name} stem...")
        _model_output, _midi_data, note_events = predict(
            tmp_path,
            model_or_model_path=ICASSP_2022_MODEL_PATH,
            onset_threshold=onset_threshold,
            frame_threshold=frame_threshold,
            minimum_note_length=minimum_note_length_ms,
            melodia_trick=True,
        )
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.remove(tmp_path)

    events: list[PitchedEvent] = []
    for note in note_events:
        start_sec = float(note[0])
        end_sec = float(note[1])
        midi_pitch = int(note[2])
        amplitude = float(note[3])
        duration = end_sec - start_sec

        events.append(PitchedEvent(
            time_sec=start_sec,
            duration_sec=duration,
            midi_pitch=midi_pitch,
            amplitude=amplitude,
            source=source_name,
        ))

    events.sort(key=lambda e: e.time_sec)
    logger.info(f"Basic Pitch ({source_name}): {len(events)} note events detected")

    if events:
        pitches = [e.midi_pitch for e in events]
        logger.info(f"  Pitch range: MIDI {min(pitches)}-{max(pitches)}, "
                     f"avg duration: {np.mean([e.duration_sec for e in events]):.3f}s")

    return events


def _onset_only_fallback(
    stem: np.ndarray,
    sr: int,
    source_name: str,
) -> list[PitchedEvent]:
    """
    Fallback when Basic Pitch is not available.
    Uses librosa onset detection + spectral centroid for rough pitch.
    """
    onset_env = librosa.onset.onset_strength(y=stem, sr=sr)
    onset_frames = librosa.onset.onset_detect(
        y=stem, sr=sr, onset_envelope=onset_env, backtrack=True
    )
    if len(onset_frames) == 0:
        return []

    onset_times = librosa.frames_to_time(onset_frames, sr=sr)
    centroids = librosa.feature.spectral_centroid(y=stem, sr=sr)[0]

    events = []
    for i, (frame, t) in enumerate(zip(onset_frames, onset_times)):
        f = min(frame, len(centroids) - 1)
        freq = centroids[f]
        midi_pitch = max(0, int(12 * np.log2(freq / 440.0 + 1e-10) + 69))

        # Estimate duration from gap to next onset
        if i < len(onset_times) - 1:
            dur = min(onset_times[i + 1] - t, 2.0) * 0.7
        else:
            dur = 0.2

        events.append(PitchedEvent(
            time_sec=float(t),
            duration_sec=dur,
            midi_pitch=midi_pitch,
            amplitude=0.7,
            source=source_name,
        ))

    return events
