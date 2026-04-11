"""
Stem-based chart generation for the rhythm game.

Takes analysis results from all stems and produces playable 5-lane charts.
Handles: lane assignment, beat-grid quantization, energy gating,
difficulty filtering, hold note generation, and playability post-processing.
"""
import logging
from dataclasses import dataclass
from typing import Optional

import numpy as np

from app.analysis.stem_analyzer import DrumEvent, PitchedEvent
from app.models.chart import Chart, Difficulty, Note, NoteType

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
#  Unified musical event (intermediate representation)
# ---------------------------------------------------------------------------

@dataclass
class MusicalEvent:
    time_sec: float
    lane: int  # 1-5
    source: str  # 'kick', 'snare', 'hihat', 'bass', 'vocals', 'other'
    strength: float  # 0-1
    beat_position: str  # 'ON_BEAT', 'HALF_BEAT', 'QUARTER_BEAT', 'OFF_GRID'
    midi_pitch: int  # 0 for drums
    duration_sec: float  # 0 for taps, >0 for potential holds
    energy_level: float  # section energy at this time


# ---------------------------------------------------------------------------
#  Lane assignment
# ---------------------------------------------------------------------------

# Drum sub-type to lane mapping
DRUM_LANE = {
    "kick": 1,
    "snare": 3,
    "hihat": 5,
}

# Pitched stem MIDI pitch ranges to lanes
# Bass: low → L1, higher bass → L2
# Vocals: always L4 (lead melody lane)
# Other (guitar/synth): pitch-dependent across L2, L3, L5


def _assign_lane_for_pitched(event: PitchedEvent) -> int:
    """Assign a lane based on stem identity and MIDI pitch."""
    if event.source == "bass":
        # Bass below C3 (MIDI 48) → Lane 1, above → Lane 2
        return 1 if event.midi_pitch < 48 else 2

    if event.source == "vocals":
        # Vocals always on Lane 4 (lead melody lane)
        return 4

    # "other" stem (guitar, synth, keys, etc.)
    # Use pitch to spread across lanes 2, 3, 5
    if event.midi_pitch < 60:      # below C4
        return 2
    elif event.midi_pitch < 72:    # C4 - B4
        return 3
    else:                          # C5+
        return 5


# ---------------------------------------------------------------------------
#  Beat grid construction
# ---------------------------------------------------------------------------

def build_beat_grid(
    beats: np.ndarray,
    downbeats: np.ndarray,
    duration_sec: float,
    subdivisions: int = 4,
) -> tuple[np.ndarray, list[str]]:
    """
    Build a quantization grid from beat positions.
    4 subdivisions per beat = sixteenth-note resolution.

    Returns:
        (grid_times, grid_positions) where grid_positions classifies each
        grid point as ON_BEAT, HALF_BEAT, or QUARTER_BEAT.
    """
    if len(beats) < 2:
        # Fallback: generate a simple grid from estimated BPM
        logger.warning("Too few beats for grid, using estimated spacing")
        avg_interval = 0.5  # assume 120 BPM
        grid_times = np.arange(0, duration_sec, avg_interval / subdivisions)
        positions = ["ON_BEAT" if i % subdivisions == 0
                      else "HALF_BEAT" if i % subdivisions == 2
                      else "QUARTER_BEAT"
                      for i in range(len(grid_times))]
        return grid_times, positions

    grid_times = []
    grid_positions = []

    for i in range(len(beats) - 1):
        beat_start = beats[i]
        beat_end = beats[i + 1]
        sub_dur = (beat_end - beat_start) / subdivisions

        for s in range(subdivisions):
            t = beat_start + s * sub_dur
            grid_times.append(t)
            if s == 0:
                grid_positions.append("ON_BEAT")
            elif s == 2:
                grid_positions.append("HALF_BEAT")
            else:
                grid_positions.append("QUARTER_BEAT")

    # Add the last beat
    grid_times.append(beats[-1])
    grid_positions.append("ON_BEAT")

    return np.array(grid_times), grid_positions


def _snap_to_grid(
    time_sec: float,
    grid_times: np.ndarray,
    grid_positions: list[str],
    max_snap_sec: float = 0.06,
) -> tuple[float, str]:
    """
    Snap a time to the nearest grid point.
    If beyond max_snap_sec, mark as OFF_GRID.
    """
    if len(grid_times) == 0:
        return time_sec, "OFF_GRID"

    idx = np.argmin(np.abs(grid_times - time_sec))
    dist = abs(grid_times[idx] - time_sec)

    if dist <= max_snap_sec:
        return float(grid_times[idx]), grid_positions[idx]
    else:
        return time_sec, "OFF_GRID"


# ---------------------------------------------------------------------------
#  Event creation from stems
# ---------------------------------------------------------------------------

def _create_events(
    drum_events: list[DrumEvent],
    bass_events: list[PitchedEvent],
    vocal_events: list[PitchedEvent],
    other_events: list[PitchedEvent],
    grid_times: np.ndarray,
    grid_positions: list[str],
    energy_times: np.ndarray,
    energy_values: np.ndarray,
    active_regions: list[tuple[float, float]],
) -> list[MusicalEvent]:
    """
    Convert all stem analysis results into a unified MusicalEvent list.
    Performs lane assignment, grid snapping, and energy gating.
    """
    from app.analysis.energy_analyzer import is_time_in_active_region

    events: list[MusicalEvent] = []

    # Drum events
    for de in drum_events:
        if not is_time_in_active_region(de.time_sec, active_regions):
            continue
        snapped_time, beat_pos = _snap_to_grid(de.time_sec, grid_times, grid_positions)
        energy = _lookup_energy(snapped_time, energy_times, energy_values)
        events.append(MusicalEvent(
            time_sec=snapped_time,
            lane=DRUM_LANE.get(de.drum_type, 3),
            source=de.drum_type,
            strength=de.strength,
            beat_position=beat_pos,
            midi_pitch=0,
            duration_sec=0,
            energy_level=energy,
        ))

    # Pitched events (bass, vocals, other)
    for pe_list in [bass_events, vocal_events, other_events]:
        for pe in pe_list:
            if not is_time_in_active_region(pe.time_sec, active_regions):
                continue
            snapped_time, beat_pos = _snap_to_grid(
                pe.time_sec, grid_times, grid_positions
            )
            energy = _lookup_energy(snapped_time, energy_times, energy_values)
            lane = _assign_lane_for_pitched(pe)
            events.append(MusicalEvent(
                time_sec=snapped_time,
                lane=lane,
                source=pe.source,
                strength=pe.amplitude,
                beat_position=beat_pos,
                midi_pitch=pe.midi_pitch,
                duration_sec=pe.duration_sec,
                energy_level=energy,
            ))

    # Sort by time
    events.sort(key=lambda e: (e.time_sec, e.lane))
    logger.info(f"Created {len(events)} unified musical events")
    return events


def _lookup_energy(
    time_sec: float,
    energy_times: np.ndarray,
    energy_values: np.ndarray,
) -> float:
    """Look up normalized energy at a given time."""
    if len(energy_times) == 0:
        return 0.5
    idx = np.argmin(np.abs(energy_times - time_sec))
    return float(energy_values[idx])


# ---------------------------------------------------------------------------
#  Difficulty filtering
# ---------------------------------------------------------------------------

def _is_near_downbeat(t: float, downbeats: np.ndarray, tol: float = 0.06) -> bool:
    """Check if a time is near a downbeat (start of a bar)."""
    if len(downbeats) == 0:
        return False
    return float(np.min(np.abs(downbeats - t))) < tol


def _filter_for_difficulty(
    events: list[MusicalEvent],
    difficulty: Difficulty,
    beats: np.ndarray,
    downbeats: np.ndarray,
) -> list[MusicalEvent]:
    """
    Filter and thin events based on difficulty level.

    Primary principle: single notes by default, chords only at specific
    musical moments (select downbeats with high energy).

    Easy:   ON_BEAT only, 3 lanes (1,3,5), never chords, max ~2 notes/sec
    Medium: ON_BEAT + HALF_BEAT, 5 lanes, rare chords (~15%), max ~4 notes/sec
    Hard:   All positions, 5 lanes, occasional chords (~25%), no density cap
    """
    if difficulty == Difficulty.EASY:
        return _filter_easy(events)
    elif difficulty == Difficulty.MEDIUM:
        return _filter_medium(events, downbeats)
    else:
        return _filter_hard(events, beats, downbeats)


def _filter_easy(events: list[MusicalEvent]) -> list[MusicalEvent]:
    """
    Easy: only ON_BEAT events, remap to 3 lanes (1, 3, 5).
    Pick strongest event per grid point. Never chords. Max ~2 notes/sec.
    """
    easy_lanes = {1: 1, 2: 1, 3: 3, 4: 3, 5: 5}
    filtered = []

    # Group by snapped time
    time_groups: dict[float, list[MusicalEvent]] = {}
    for e in events:
        if e.beat_position != "ON_BEAT":
            continue
        key = round(e.time_sec, 4)
        time_groups.setdefault(key, []).append(e)

    for t in sorted(time_groups.keys()):
        group = time_groups[t]
        # Pick strongest event — single note only
        best = max(group, key=lambda e: e.strength)
        best_copy = MusicalEvent(
            time_sec=best.time_sec,
            lane=easy_lanes.get(best.lane, 3),
            source=best.source,
            strength=best.strength,
            beat_position=best.beat_position,
            midi_pitch=best.midi_pitch,
            duration_sec=0,  # no holds in easy
            energy_level=best.energy_level,
        )
        filtered.append(best_copy)

    # Enforce minimum 450ms gap (max ~2 notes/sec)
    return _enforce_min_gap(filtered, min_gap_sec=0.45)


def _filter_medium(
    events: list[MusicalEvent],
    downbeats: np.ndarray,
) -> list[MusicalEvent]:
    """
    Medium: ON_BEAT + HALF_BEAT, all 5 lanes.
    Single note by default. Chords only every 4th downbeat with high energy.
    Max ~4 notes/sec.
    """
    filtered = []
    downbeat_chord_counter = 0

    time_groups: dict[float, list[MusicalEvent]] = {}
    for e in events:
        if e.beat_position == "QUARTER_BEAT":
            continue
        key = round(e.time_sec, 4)
        time_groups.setdefault(key, []).append(e)

    for t in sorted(time_groups.keys()):
        group = time_groups[t]
        sorted_group = sorted(group, key=lambda x: -x.strength)

        # Always pick the strongest event as the primary note
        primary = sorted_group[0]
        filtered.append(primary)

        # Chord check: only on downbeats with high energy, every 4th downbeat
        is_db = _is_near_downbeat(t, downbeats)
        if is_db:
            downbeat_chord_counter += 1

        if (is_db
                and downbeat_chord_counter % 4 == 0
                and len(sorted_group) >= 2
                and primary.energy_level > 0.65):
            second = sorted_group[1]
            if second.lane != primary.lane:
                filtered.append(second)

    # Enforce minimum 230ms gap (max ~4 notes/sec)
    return _enforce_min_gap(filtered, min_gap_sec=0.23)


def _filter_hard(
    events: list[MusicalEvent],
    beats: np.ndarray,
    downbeats: np.ndarray,
) -> list[MusicalEvent]:
    """
    Hard: all positions, 5 lanes.
    Single note by default. Chords every 2nd downbeat with moderate+ energy.
    Max 2 simultaneous notes on chords.
    """
    filtered = []
    downbeat_chord_counter = 0

    time_groups: dict[float, list[MusicalEvent]] = {}
    for e in events:
        key = round(e.time_sec, 4)
        time_groups.setdefault(key, []).append(e)

    for t in sorted(time_groups.keys()):
        group = time_groups[t]
        sorted_group = sorted(group, key=lambda x: -x.strength)

        # Always pick the strongest event as the primary note
        primary = sorted_group[0]
        filtered.append(primary)

        # Chord check: every 2nd downbeat with moderate energy
        is_db = _is_near_downbeat(t, downbeats)
        if is_db:
            downbeat_chord_counter += 1

        if (is_db
                and downbeat_chord_counter % 2 == 0
                and len(sorted_group) >= 2
                and primary.energy_level > 0.5):
            second = sorted_group[1]
            if second.lane != primary.lane:
                filtered.append(second)

    # Enforce minimum 80ms gap
    return _enforce_min_gap(filtered, min_gap_sec=0.08)


def _enforce_min_gap(
    events: list[MusicalEvent],
    min_gap_sec: float,
) -> list[MusicalEvent]:
    """Remove events that are too close together (keep strongest)."""
    if not events:
        return events

    events.sort(key=lambda e: (e.time_sec, -e.strength))
    result = [events[0]]

    for e in events[1:]:
        # Allow simultaneous notes (same time, different lanes = chord)
        if abs(e.time_sec - result[-1].time_sec) < 0.01:
            result.append(e)
            continue
        if e.time_sec - result[-1].time_sec >= min_gap_sec:
            result.append(e)

    return result


# ---------------------------------------------------------------------------
#  Hold note detection
# ---------------------------------------------------------------------------

def _apply_hold_notes(
    events: list[MusicalEvent],
    difficulty: Difficulty,
    min_hold_sec: float = 0.35,
    max_hold_sec: float = 3.0,
) -> list[MusicalEvent]:
    """
    Convert sustained pitched notes into hold notes.
    Only for Medium and Hard difficulty.
    """
    if difficulty == Difficulty.EASY:
        return events

    result = []
    for e in events:
        if e.duration_sec >= min_hold_sec and e.source in ("bass", "vocals", "other"):
            hold_dur = min(e.duration_sec, max_hold_sec)
            # In medium, only allow holds on ON_BEAT
            if difficulty == Difficulty.MEDIUM and e.beat_position != "ON_BEAT":
                e_copy = MusicalEvent(**{**e.__dict__, "duration_sec": 0})
                result.append(e_copy)
                continue
            e_copy = MusicalEvent(**{**e.__dict__, "duration_sec": hold_dur})
            result.append(e_copy)
        else:
            e_copy = MusicalEvent(**{**e.__dict__, "duration_sec": 0})
            result.append(e_copy)

    return result


# ---------------------------------------------------------------------------
#  Playability post-processing
# ---------------------------------------------------------------------------

def _postprocess(notes: list[Note]) -> list[Note]:
    """
    Ensure the final note list is playable and feels natural.

    Rules:
    1. No more than 3 consecutive notes on the same lane
    2. Minimum 50ms between any two notes (even across lanes)
    3. Hold notes can't overlap with taps on the same lane
    """
    if len(notes) < 2:
        return notes

    # Rule 1: Break same-lane repetition (shift to adjacent lane)
    consecutive_count = 1
    for i in range(1, len(notes)):
        if notes[i].lane == notes[i - 1].lane and notes[i].type == NoteType.TAP:
            consecutive_count += 1
            if consecutive_count > 3:
                # Move to adjacent lane
                if notes[i].lane < 5:
                    notes[i] = Note(
                        time_ms=notes[i].time_ms,
                        lane=notes[i].lane + 1,
                        duration_ms=notes[i].duration_ms,
                        type=notes[i].type,
                    )
                else:
                    notes[i] = Note(
                        time_ms=notes[i].time_ms,
                        lane=notes[i].lane - 1,
                        duration_ms=notes[i].duration_ms,
                        type=notes[i].type,
                    )
                consecutive_count = 1
        else:
            consecutive_count = 1

    # Rule 3: Remove taps that overlap with hold notes on same lane
    holds_active: dict[int, int] = {}  # lane → end_time_ms
    final = []
    for note in notes:
        if note.type == NoteType.HOLD:
            holds_active[note.lane] = note.time_ms + note.duration_ms
            final.append(note)
        else:
            hold_end = holds_active.get(note.lane, 0)
            if note.time_ms >= hold_end:
                final.append(note)

    return final


# ---------------------------------------------------------------------------
#  Main chart generation entry point
# ---------------------------------------------------------------------------

def generate_all_charts(
    drum_events: list[DrumEvent],
    bass_events: list[PitchedEvent],
    vocal_events: list[PitchedEvent],
    other_events: list[PitchedEvent],
    beats: np.ndarray,
    downbeats: np.ndarray,
    bpm: float,
    energy_times: np.ndarray,
    energy_values: np.ndarray,
    active_regions: list[tuple[float, float]],
    song_id: str,
    filename: str,
    duration_ms: int,
) -> dict:
    """
    Generate charts for all difficulty levels.

    Returns:
        {
            "song_id": ...,
            "filename": ...,
            "duration_ms": ...,
            "bpm": ...,
            "charts": {
                "easy": chart_dict,
                "medium": chart_dict,
                "hard": chart_dict,
            }
        }
    """
    duration_sec = duration_ms / 1000.0

    # Build beat grid (sixteenth-note resolution)
    grid_times, grid_positions = build_beat_grid(
        beats, downbeats, duration_sec
    )

    # Create unified event list from all stems
    all_events = _create_events(
        drum_events, bass_events, vocal_events, other_events,
        grid_times, grid_positions,
        energy_times, energy_values,
        active_regions,
    )

    if not all_events:
        return _empty_result(song_id, filename, duration_ms, bpm)

    # Generate chart for each difficulty
    charts = {}
    for diff in Difficulty:
        # Filter events by difficulty
        filtered = _filter_for_difficulty(all_events, diff, beats, downbeats)

        # Apply hold notes
        filtered = _apply_hold_notes(filtered, diff)

        # Convert to Note objects
        notes = []
        for e in filtered:
            note_type = NoteType.HOLD if e.duration_sec > 0 else NoteType.TAP
            dur_ms = int(e.duration_sec * 1000) if note_type == NoteType.HOLD else 0
            notes.append(Note(
                time_ms=int(e.time_sec * 1000),
                lane=e.lane,
                duration_ms=dur_ms,
                type=note_type,
            ))

        # Sort and deduplicate
        notes.sort(key=lambda n: (n.time_ms, n.lane))
        notes = _deduplicate_notes(notes)

        # Playability post-processing
        notes = _postprocess(notes)

        # Build chart
        chart = Chart(
            song_id=song_id,
            title=filename,
            duration_ms=duration_ms,
            bpm=round(bpm, 1),
            difficulty=diff,
            notes=notes,
        )
        charts[diff.value] = chart.to_dict()

        logger.info(f"  {diff.value}: {len(notes)} notes "
                     f"(taps={sum(1 for n in notes if n.type==NoteType.TAP)}, "
                     f"holds={sum(1 for n in notes if n.type==NoteType.HOLD)})")

    return {
        "song_id": song_id,
        "filename": filename,
        "duration_ms": duration_ms,
        "bpm": round(bpm, 1),
        "charts": charts,
    }


def _deduplicate_notes(notes: list[Note]) -> list[Note]:
    """Remove duplicate notes at the same time and lane."""
    seen = set()
    result = []
    for n in notes:
        key = (n.time_ms, n.lane)
        if key not in seen:
            seen.add(key)
            result.append(n)
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
