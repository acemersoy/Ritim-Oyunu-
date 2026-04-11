# Rhythm Game Mobile – Backend & Frontend

This repository contains the full code‑base for a **Rhythm Game Mobile** project.
It is split into two main parts:

1. **Backend** – A FastAPI service that accepts audio uploads, analyses the
   waveform, generates beat maps, and stores the results in a SQLite database.
2. **Frontend** – A React (Vite) single‑page application that consumes the API
   and lets users play, share, and download charts.

The project is developed on Windows but the code is fully cross‑platform.

---

## 📦 Project structure

```
backend/          # Python FastAPI backend
   app/            # FastAPI application code
   tests/          # pytest tests for the analysis pipeline
   requirements.txt
neon-rhythm-studio/  # React + Vite front‑end
   src/            # React components & hooks
   package.json
RhythmGame_Technical_Specification_v2.md
rhythm_game_documentation.md
README.md
```

## ⚙️ Prerequisites

| Component | Minimum version | Install command |
|-----------|-----------------|----------------|
| Python    | 3.10+           | `py -3.10 -m venv venv` |
| Node.js   | 20+             | `winget install OpenJS.NodeJS` (or download from nodejs.org) |
| SQLite3   | 3.31+           | Built‑in with Python’s `sqlite3` module |

## 🚀 Getting started

### 1️⃣ Clone the repo

```sh
git clone https://github.com/<your-org>/rhythm-game-mobile.git
cd rhythm-game-mobile
```

### 2️⃣ Backend – API server

```sh
cd backend
python -m venv .venv
.
.venv\Scripts\activate  # Windows
python -m pip install -r requirements.txt
uvicorn main:app --reload
```

The server will start on `http://localhost:8000`. Open the OpenAPI docs at
`http://localhost:8000/docs` to explore the available endpoints.

### 3️⃣ Frontend – UI

```sh
cd neon-rhythm-studio
npm install
npm run dev
```

The dev server runs at `http://localhost:5173`. It proxies API requests to the
backend automatically.

### 4️⃣ Running tests

```sh
cd backend
pytest
```

All tests are located under `backend/tests`. They verify the audio analysis
pipeline and data models.

## 📚 API Overview

| Method | Path | Description | Request Body | Response |
|--------|------|-------------|--------------|----------|
| POST | `/upload` | Upload an audio file and start analysis. | `multipart/form‑data` – `file` | `UploadResponse` (song_id, status) |
| GET | `/status/{song_id}` | Get analysis status. | – | `StatusResponse` |
| GET | `/chart/{song_id}` | Retrieve the generated chart. | – | `ChartResponse` |
| POST | `/share` | Create a shareable link. | `ShareRequest` | `ShareResponse` |
| GET | `/share/{token}` | Retrieve chart via share token. | – | `ChartResponse` |
| GET | `/songs` | List all uploaded songs. | – | `List[SongListItem]` |
| GET | `/audio/{song_id}` | Download the original audio. | – | `FileResponse` |
| DELETE | `/songs/{song_id}` | Delete a song and its chart. | – | `DeleteResponse` |

See the OpenAPI docs for detailed request/response schemas.

## 🔧 Architecture

### Backend

* **FastAPI** – High‑performance async web framework.
* **SQLAlchemy + aiosqlite** – ORM over SQLite.
* **Audio analysis pipeline** – A set of pure functions:
  * `onset_detector.py` – Detect beats and note onsets.
  * `pitch_detector.py` – Identify pitches.
  * `beat_tracker.py` – Compute BPM and beat positions.
  * `lane_mapper.py` – Map frequencies to game lanes.
  * `difficulty.py` – Filter notes for a chosen difficulty.
* Background tasks run in a thread pool to keep the HTTP thread responsive.

### Frontend

* **React (Vite)** – Modern SPA framework.
* **Context + hooks** – Global state for user session and song list.
* **Chart renderer** – Lightweight canvas component that draws the notes.

## 📄 Documentation

* **Technical Specification** – `RhythmGame_Technical_Specification_v2.md`.
* **Developer Guide** – `rhythm_game_developer_guide_v3.md`.
* **Project Kapsamli Technical Document** – `Rhythm_Game_Mobile_Kapsamli_Teknik_Dokuman_v2.md`.

These documents contain detailed architecture diagrams, API contracts, and
implementation notes.

## 🤝 Contributing

1. Fork the repository and create a feature branch.
2. Run the test suite locally to ensure your changes don’t break existing
   behaviour.
3. Submit a pull request with a clear description of the issue addressed.
4. Follow the code style used throughout the project (PEP 8 for Python,
   Prettier/ESLint for JavaScript).

All contributions are welcome – whether it’s a bug fix, a new feature, or
documentation improvement.

## 📄 License

This project is open source. The source code is available under the MIT
License. See the `LICENSE` file for details.

---

**Happy coding!**

