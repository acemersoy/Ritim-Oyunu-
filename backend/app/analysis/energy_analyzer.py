"""
Energy analysis and section detection for the full mix.

Provides:
- RMS energy curve (for gating silent/ambient sections)
- Active region detection (suppress notes where there's no musical energy)
- Energy-based section estimation (verse/chorus/break proxy)
"""
import logging
import numpy as np
import librosa

logger = logging.getLogger(__name__)


def compute_energy_curve(
    y: np.ndarray,
    sr: int,
    hop_length: int = 512,
) -> tuple[np.ndarray, np.ndarray]:
    """
    Compute the RMS energy envelope of the full mix.

    Returns:
        (times, rms_values) both as 1-D numpy arrays.
        times is in seconds, rms_values normalized to 0-1.
    """
    rms = librosa.feature.rms(y=y, hop_length=hop_length)[0]
    times = librosa.frames_to_time(np.arange(len(rms)), sr=sr, hop_length=hop_length)

    # Normalize to 0-1
    max_rms = rms.max()
    if max_rms > 0:
        rms = rms / max_rms

    return times, rms


def get_energy_at_time(
    time_sec: float,
    energy_times: np.ndarray,
    energy_values: np.ndarray,
) -> float:
    """Look up energy value at a specific time (nearest frame)."""
    if len(energy_times) == 0:
        return 0.0
    idx = np.argmin(np.abs(energy_times - time_sec))
    return float(energy_values[idx])


def detect_active_regions(
    y: np.ndarray,
    sr: int,
    silence_threshold_db: float = -40.0,
    min_region_sec: float = 1.0,
    hop_length: int = 512,
) -> list[tuple[float, float]]:
    """
    Detect time regions where there is meaningful musical activity.
    Regions below the energy threshold are considered silent/ambient.

    Args:
        silence_threshold_db: dB below peak RMS to consider as silence.
        min_region_sec: Minimum duration for a region to be kept.

    Returns:
        List of (start_sec, end_sec) tuples for active regions.
    """
    rms = librosa.feature.rms(y=y, hop_length=hop_length)[0]
    times = librosa.frames_to_time(np.arange(len(rms)), sr=sr, hop_length=hop_length)

    if len(rms) == 0 or rms.max() == 0:
        return [(0.0, librosa.get_duration(y=y, sr=sr))]

    # Convert threshold from dB relative to peak
    peak_rms = rms.max()
    threshold = peak_rms * (10 ** (silence_threshold_db / 20.0))

    # Find frames above threshold (smoothed to avoid flickering)
    # Apply a short median filter to smooth the energy curve
    kernel_size = max(3, int(0.2 * sr / hop_length))  # ~200ms
    if kernel_size % 2 == 0:
        kernel_size += 1
    from scipy.ndimage import median_filter
    rms_smooth = median_filter(rms, size=kernel_size)

    active = rms_smooth >= threshold

    # Extract contiguous active regions
    regions = []
    in_region = False
    region_start = 0.0

    for i, is_active in enumerate(active):
        t = times[i] if i < len(times) else times[-1]
        if is_active and not in_region:
            region_start = t
            in_region = True
        elif not is_active and in_region:
            if t - region_start >= min_region_sec:
                regions.append((region_start, t))
            in_region = False

    # Close final region
    if in_region:
        final_t = librosa.get_duration(y=y, sr=sr)
        if final_t - region_start >= min_region_sec:
            regions.append((region_start, final_t))

    if not regions:
        # If everything is below threshold, use the entire song
        regions = [(0.0, librosa.get_duration(y=y, sr=sr))]

    logger.info(f"Active regions: {len(regions)} "
                f"(covering {sum(e-s for s,e in regions):.1f}s "
                f"of {librosa.get_duration(y=y, sr=sr):.1f}s total)")

    return regions


def compute_section_energy_map(
    y: np.ndarray,
    sr: int,
    window_sec: float = 4.0,
    hop_sec: float = 2.0,
) -> list[tuple[float, float, float]]:
    """
    Compute a coarse energy map over the song, useful for
    adapting note density to musical sections.

    Returns:
        List of (center_time, energy_normalized, spectral_complexity)
        where energy_normalized is 0-1 and spectral_complexity captures
        how "busy" the section is (more instruments / higher complexity = higher).
    """
    duration = librosa.get_duration(y=y, sr=sr)
    window_samples = int(window_sec * sr)
    hop_samples = int(hop_sec * sr)

    sections = []
    pos = 0
    while pos + window_samples <= len(y):
        segment = y[pos:pos + window_samples]
        center_time = (pos + window_samples / 2) / sr

        # RMS energy
        rms = np.sqrt(np.mean(segment ** 2))

        # Spectral complexity: spectral flux captures how much the
        # spectrum changes within the window (more instruments = more flux)
        S = np.abs(librosa.stft(segment, n_fft=1024, hop_length=256))
        flux = np.mean(np.sum(np.diff(S, axis=1) ** 2, axis=0))

        sections.append((center_time, rms, flux))
        pos += hop_samples

    if not sections:
        return [(duration / 2, 0.5, 0.5)]

    # Normalize
    max_rms = max(s[1] for s in sections) or 1.0
    max_flux = max(s[2] for s in sections) or 1.0

    normalized = [
        (t, rms / max_rms, flux / max_flux)
        for t, rms, flux in sections
    ]

    return normalized


def is_time_in_active_region(
    time_sec: float,
    active_regions: list[tuple[float, float]],
) -> bool:
    """Check if a given time falls within any active region."""
    for start, end in active_regions:
        if start <= time_sec <= end:
            return True
    return False
