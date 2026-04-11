"""
Source separation using Demucs v4 (htdemucs).
Separates a full stereo mix into 4 stems: drums, bass, vocals, other.

Demucs is a state-of-the-art neural network model by Meta Research
that achieves the highest Source-to-Distortion Ratio (SDR) among
open-source separation models.
"""
import gc
import logging
import numpy as np

logger = logging.getLogger(__name__)

# Module-level model cache (loaded once, reused across analyses)
_demucs_model = None


def _get_model():
    """Lazy-load and cache the Demucs htdemucs model."""
    global _demucs_model
    if _demucs_model is not None:
        return _demucs_model

    import torch
    from demucs.pretrained import get_model

    logger.info("Loading Demucs htdemucs model (first call, will be cached)...")
    model = get_model("htdemucs")
    model.eval()

    if torch.cuda.is_available():
        model = model.cuda()
        logger.info("Demucs model loaded on GPU")
    else:
        logger.info("Demucs model loaded on CPU")

    _demucs_model = model
    return model


def separate_stems(wav_path: str) -> tuple[dict[str, np.ndarray], int]:
    """
    Separate audio into 4 stems using Demucs htdemucs.

    Args:
        wav_path: Path to the WAV file (any sample rate, will be resampled).

    Returns:
        (stems, sample_rate) where stems is a dict mapping stem name
        ('drums', 'bass', 'vocals', 'other') to a mono float32 numpy array
        at the model's native sample rate (44100 Hz).
    """
    import torch
    import soundfile as sf
    from demucs.apply import apply_model

    model = _get_model()
    device = next(model.parameters()).device

    logger.info(f"Separating stems for: {wav_path}")

    # Load with soundfile (avoids torchaudio torchcodec dependency)
    audio_np, sr = sf.read(wav_path, dtype="float32")
    # soundfile returns (samples, channels) — transpose to (channels, samples)
    if audio_np.ndim == 1:
        wav = torch.from_numpy(audio_np).unsqueeze(0)  # mono → (1, samples)
    else:
        wav = torch.from_numpy(audio_np.T)  # (channels, samples)

    # Resample to model's expected rate (44100 Hz)
    target_sr = model.samplerate
    if sr != target_sr:
        logger.info(f"Resampling {sr} -> {target_sr} Hz for Demucs")
        import torchaudio
        wav = torchaudio.functional.resample(wav, sr, target_sr)

    # Ensure stereo (Demucs expects exactly 2 channels)
    if wav.shape[0] == 1:
        wav = wav.repeat(2, 1)
    elif wav.shape[0] > 2:
        wav = wav[:2]

    # Normalize for stable model input
    ref = wav.mean(0)
    ref_mean = ref.mean()
    ref_std = ref.std() + 1e-8
    wav_norm = (wav - ref_mean) / ref_std

    logger.info("Running Demucs separation (this may take a few minutes on CPU)...")
    wav_input = wav_norm[None].to(device)  # add batch dim

    with torch.no_grad():
        sources = apply_model(
            model,
            wav_input,
            device=device,
            progress=False,
            num_workers=0,
        )[0]  # remove batch dim → (n_sources, channels, samples)

    # Denormalize
    sources = sources * ref_std + ref_mean

    # Convert each source to mono numpy float32
    stems = {}
    for i, name in enumerate(model.sources):
        stem_stereo = sources[i].cpu().numpy()  # (2, samples)
        stem_mono = stem_stereo.mean(axis=0).astype(np.float32)
        peak = np.abs(stem_mono).max()
        logger.info(f"  {name}: {len(stem_mono)} samples, peak={peak:.4f}")
        stems[name] = stem_mono

    # Free memory
    del sources, wav_input, wav_norm, wav
    if torch.cuda.is_available():
        torch.cuda.empty_cache()
    gc.collect()

    logger.info("Demucs separation complete")
    return stems, target_sr


def separate_stems_fallback(y: np.ndarray, sr: int) -> dict[str, np.ndarray]:
    """
    Fallback separation using librosa HPSS + band-pass filtering.
    Used when Demucs is unavailable or fails.
    Much less accurate but zero extra dependencies.
    """
    import librosa

    logger.warning("Using HPSS fallback (Demucs unavailable)")

    y_harmonic, y_percussive = librosa.effects.hpss(y, margin=3.0)

    S_harm = librosa.stft(y_harmonic, n_fft=2048)
    freqs = librosa.fft_frequencies(sr=sr, n_fft=2048)

    def _band_extract(S, freqs, lo, hi, length):
        mask = ((freqs >= lo) & (freqs <= hi)).astype(np.float32)
        return librosa.istft(S * mask[:, np.newaxis], length=length).astype(np.float32)

    stems = {
        "drums": y_percussive.astype(np.float32),
        "bass": _band_extract(S_harm, freqs, 20, 250, len(y)),
        "vocals": _band_extract(S_harm, freqs, 250, 4000, len(y)),
        "other": _band_extract(S_harm, freqs, 4000, sr / 2, len(y)),
    }
    return stems
