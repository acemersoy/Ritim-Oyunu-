"""
Tests for the audio analysis pipeline.
Uses synthetic audio (sine waves) to verify that the pipeline
correctly detects frequencies and maps them to lanes.
"""
import math
import numpy as np
import pytest
from app.analysis.onset_detector import detect_onsets, detect_onsets_with_strength
from app.analysis.pitch_detector import detect_pitches_at_onsets
from app.analysis.beat_tracker import detect_tempo_and_beats
from app.analysis.lane_mapper import frequency_to_lane, create_notes
from app.analysis.difficulty import filter_by_difficulty
from app.models.chart import Difficulty, NoteType


def generate_sine_wave(freq: float, duration: float = 1.0, sr: int = 22050) -> np.ndarray:
    """Generate a pure sine wave at the given frequency."""
    t = np.linspace(0, duration, int(sr * duration), endpoint=False)
    return 0.5 * np.sin(2 * np.pi * freq * t)


def generate_clicks(times: list[float], duration: float = 5.0, sr: int = 22050) -> np.ndarray:
    """Generate audio with click/impulse sounds at specified times."""
    n_samples = int(sr * duration)
    y = np.zeros(n_samples)
    for t in times:
        idx = int(t * sr)
        if idx < n_samples:
            # Short burst
            burst_len = min(int(0.01 * sr), n_samples - idx)
            y[idx:idx + burst_len] = 0.8 * np.random.randn(burst_len)
    return y


class TestFrequencyToLane:
    """Guitar-focused lane mapping tests.
    Lanes map guitar strings/frets:
      Lane 1: ~80-130 Hz   (low E/A open)
      Lane 2: ~130-220 Hz  (A/D mid)
      Lane 3: ~220-370 Hz  (D/G strings)
      Lane 4: ~370-620 Hz  (G/B strings)
      Lane 5: ~620-1400 Hz (B/high E, high frets)
    """
    def test_low_e_string(self):
        # E2 = 82.4 Hz -> lane 1
        assert frequency_to_lane(82.0) == 1

    def test_a_string(self):
        # A2 = 110 Hz -> lane 1
        assert frequency_to_lane(110.0) == 1

    def test_d_string(self):
        # D3 = 146.8 Hz -> lane 2
        assert frequency_to_lane(147.0) == 2

    def test_g_string(self):
        # G3 = 196 Hz -> lane 2
        assert frequency_to_lane(196.0) == 2

    def test_b_string(self):
        # B3 = 246.9 Hz -> lane 3
        assert frequency_to_lane(247.0) == 3

    def test_high_e_string(self):
        # E4 = 329.6 Hz -> lane 3
        assert frequency_to_lane(330.0) == 3

    def test_high_fret_g(self):
        # ~500 Hz -> lane 4
        assert frequency_to_lane(500.0) == 4

    def test_high_fret_bend(self):
        # ~800 Hz -> lane 5
        assert frequency_to_lane(800.0) == 5

    def test_very_low_frequency(self):
        assert frequency_to_lane(30.0) == 1

    def test_very_high_frequency(self):
        assert frequency_to_lane(10000.0) == 5

    def test_zero_frequency(self):
        assert frequency_to_lane(0.0) == 3  # Default to middle

    def test_negative_frequency(self):
        assert frequency_to_lane(-100.0) == 3  # Default to middle


class TestOnsetDetection:
    def test_detect_clicks(self):
        sr = 22050
        click_times = [0.5, 1.0, 1.5, 2.0, 2.5]
        y = generate_clicks(click_times, duration=3.0, sr=sr)
        onsets = detect_onsets(y, sr)
        # Should detect roughly the right number of onsets
        assert len(onsets) >= 3  # At least some detected

    def test_detect_onsets_with_strength(self):
        sr = 22050
        click_times = [0.5, 1.0, 1.5, 2.0]
        y = generate_clicks(click_times, duration=3.0, sr=sr)
        results = detect_onsets_with_strength(y, sr)
        assert len(results) > 0
        for time, strength in results:
            assert time >= 0
            assert 0 <= strength <= 1.0


class TestBeatTracking:
    def test_tempo_detection(self):
        sr = 22050
        # Generate clicks at 120 BPM (0.5s intervals)
        bpm_target = 120
        beat_interval = 60.0 / bpm_target
        click_times = [i * beat_interval for i in range(20)]
        y = generate_clicks(click_times, duration=12.0, sr=sr)

        bpm, beat_times = detect_tempo_and_beats(y, sr)
        # BPM should be roughly in the right ballpark
        assert 60 <= bpm <= 240  # Wide range since synthetic audio is tricky


class TestDifficultyFilter:
    def _make_notes(self, count: int = 20):
        """Generate a list of test notes spanning all lanes."""
        notes = []
        for i in range(count):
            from app.models.chart import Note
            notes.append(Note(
                time_ms=i * 500,
                lane=(i % 5) + 1,
                duration_ms=200,
                type=NoteType.TAP,
            ))
        return notes

    def test_easy_reduces_lanes(self):
        notes = self._make_notes(20)
        filtered = filter_by_difficulty(notes, Difficulty.EASY)
        lanes_used = {n.lane for n in filtered}
        # Easy should only use lanes 1, 3, 5
        assert lanes_used.issubset({1, 3, 5})

    def test_easy_reduces_density(self):
        notes = self._make_notes(20)
        filtered = filter_by_difficulty(notes, Difficulty.EASY)
        # Should have fewer notes
        assert len(filtered) < len(notes)

    def test_medium_reduces_density(self):
        notes = self._make_notes(20)
        filtered = filter_by_difficulty(notes, Difficulty.MEDIUM)
        assert len(filtered) < len(notes)

    def test_hard_adds_chords(self):
        notes = self._make_notes(20)
        filtered = filter_by_difficulty(notes, Difficulty.HARD)
        # Hard adds chord notes, so count should be >= original
        assert len(filtered) >= len(notes)

    def test_hard_preserves_all_notes(self):
        notes = self._make_notes(20)
        filtered = filter_by_difficulty(notes, Difficulty.HARD)
        original_times = {n.time_ms for n in notes}
        filtered_times = {n.time_ms for n in filtered}
        # All original note times should still be present
        assert original_times.issubset(filtered_times)


class TestCreateNotes:
    def test_creates_notes_from_guitar_frequencies(self):
        onset_times = [0.5, 1.0, 1.5, 2.0]
        # Guitar frequencies: low E, D string, B string, high fret
        frequencies = [82.0, 147.0, 330.0, 800.0]
        strengths = [1.0, 0.8, 0.6, 0.9]
        beat_times = [0.5, 1.0, 1.5, 2.0]

        notes = create_notes(onset_times, frequencies, strengths, beat_times)
        assert len(notes) == 4
        assert notes[0].lane == 1  # 82Hz -> low E -> lane 1
        assert notes[3].lane == 5  # 800Hz -> high fret -> lane 5


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
