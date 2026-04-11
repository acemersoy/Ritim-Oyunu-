FROM python:3.11-slim

# System dependencies:
#   ffmpeg      — audio format conversion (pydub)
#   libsndfile1 — audio I/O (soundfile)
#   build-essential — C extension compilation (madmom if added later)
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        ffmpeg \
        libsndfile1 \
        build-essential \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install CPU-only PyTorch first (saves ~2GB vs CUDA version).
# Demucs requires torch+torchaudio; installing CPU variants explicitly
# prevents pip from pulling the full CUDA bundle.
RUN pip install --no-cache-dir \
    torch \
    torchaudio \
    --index-url https://download.pytorch.org/whl/cpu

# Install Python dependencies.
# torch is already satisfied from above, so demucs won't reinstall it.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt \
    --extra-index-url https://download.pytorch.org/whl/cpu

# Pre-download ML models during build so first analysis doesn't stall.
# Demucs htdemucs model (~80MB)
RUN python -c "\
from demucs.pretrained import get_model; \
model = get_model('htdemucs'); \
print(f'Demucs htdemucs cached. Sources: {model.sources}') \
"

# Basic Pitch ONNX model
RUN python -c "\
from basic_pitch import ICASSP_2022_MODEL_PATH; \
print(f'Basic Pitch model path: {ICASSP_2022_MODEL_PATH}') \
"

COPY . .

# Create data directories
RUN mkdir -p /app/data/uploads /app/data/wav /app/data/stems

EXPOSE 8000

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
