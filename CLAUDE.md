# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Rhythm Game Mobile: three cooperating codebases.

- **`android/`** — Kotlin/Jetpack Compose Android rhythm game (primary client). **Locked to landscape** (`sensorLandscape` in `AndroidManifest.xml`).
- **`backend/`** — Python/FastAPI service that ingests audio, runs an analysis pipeline, and returns note charts.
- **`neon-rhythm-studio/`** — React 19 + Vite 6 + TypeScript web client (secondary/experimental UI that consumes the same backend API).

Users upload music, the backend analyzes audio (librosa + pydub) to generate charts, then the Android app renders a 5-lane rhythm game.

## Build & Run Commands

### Android
```bash
cd android
export JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"  # Windows required
./gradlew :app:compileDebugKotlin  # Fast compile check (preferred during iteration)
./gradlew assembleDebug            # Build debug APK
./gradlew assembleRelease          # Build release APK (ProGuard enabled)
./gradlew test                     # Unit tests
./gradlew connectedAndroidTest     # Instrumented tests
```
- APK output: `android/app/build/outputs/apk/debug/app-debug.apk`
- minSdk 26, targetSdk 34, compileSdk 34, Java 17, Kotlin Compose compiler 1.5.14
- **API base URL is hardcoded in `android/app/build.gradle.kts`** as `BuildConfig.BASE_URL` for both debug and release. It currently points to a Cloudflare tunnel (`*.trycloudflare.com/api/`). Update this field when the tunnel rotates or when targeting a local backend (e.g. `http://10.0.2.2:8000/api/` for the Android emulator).

### Backend
```bash
cd backend
python -m venv .venv && .venv\Scripts\activate  # Windows
pip install -r requirements.txt
uvicorn main:app --reload --host 0.0.0.0 --port 8000
pytest                                          # Run analysis pipeline tests
pytest tests/test_analysis.py::test_name        # Run a single test
```
Docker: `cd backend && docker-compose up --build` (Dockerfile ships with ffmpeg baked in, required by pydub).

Health check: `GET /api/health`. OpenAPI docs: `http://localhost:8000/docs`.

### React Web Client (`neon-rhythm-studio`)
```bash
cd neon-rhythm-studio
npm install
npm run dev       # Vite dev server on :5173
npm run build     # Production build
npm run preview   # Preview production build
```
No test script configured. Uses React 19 + TypeScript 5.8 + Vite 6. Consumes the same FastAPI backend as the Android client.

## High-Level Architecture

### Backend (FastAPI)
- Entry point: `backend/main.py` — lifespan context manager initializes the SQLite DB.
- REST routes: `backend/app/api/routes.py`, all prefixed `/api`. Key endpoints: `POST /upload`, `GET /status/{songId}`, `GET /chart/{songId}?difficulty`, `GET /audio/{songId}`, `GET /songs`, `DELETE /songs/{songId}`.
- Audio analysis pipeline in `backend/app/analysis/pipeline.py` orchestrates (in order): `audio_loader` → `guitar_isolator` → `onset_detector` → `pitch_detector` → `beat_tracker` → `lane_mapper` → `difficulty`.
- Analysis runs inside a `ThreadPoolExecutor` so HTTP threads stay responsive while heavy librosa work happens in the background.
- Persistence: SQLAlchemy async + aiosqlite. DB lives at `backend/rhythm_game.db`.
- Config: `backend/app/config.py` — environment-driven `Settings` class (HOST, PORT, CORS_ORIGINS, UPLOAD_DIR, MAX_UPLOAD_MB=200).
- Analysis unit tests live in `backend/tests/test_analysis.py`.

### Android (Kotlin/Compose)
Package root: `com.rhythmgame` under `android/app/src/main/java/com/rhythmgame/`.

- **DI** (Hilt): `di/NetworkModule.kt` provides Retrofit/OkHttp + `BASE_URL` from `BuildConfig`; `di/AppModule.kt` provides Room DB, DAOs, and the application-scoped `Context`.
- **Data layer**: Repository pattern in `data/repository/SongRepository.kt` wraps Room (local cache) + Retrofit (remote). `data/worker/` holds WorkManager jobs for background sync/download.
- **Navigation**: Compose Navigation with typed routes in `navigation/NavRoutes.kt` and the graph in `navigation/AppNavGraph.kt`. Screens: Splash → Home → (SongList | Upload | Settings | Ranked | Profile | Multiplayer | Store) → SongDetail → Game → Result.
- **Single Activity**: `MainActivity` enables edge-to-edge, hides system bars with `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, and re-hides on focus regain. The whole app is landscape-locked by manifest; `configChanges="orientation|screenSize|keyboardHidden"` prevents activity recreation on rotation.
- **Landscape layouts**: `HomeScreen` is a 2-column `Row` (left: 3 primary buttons, right: store/profile with badges) — not scrollable. `SongDetailScreen` is also a 2-column `Row` (left: square aspect-ratio cover + song info, right: difficulty rows + start button) — no vertical scroll required. `GameScreen`'s pause overlay is a 2-column layout (left: PAUSED title + song card, right: Resume/Restart/Quit stack).

### Game Engine (`game/` package)
The engine runs on a **dedicated `Thread` with `Thread.MAX_PRIORITY`** (not Choreographer) for uncapped frame rate. Key files:
- `GameEngine.kt` — game loop thread, touch input dispatch, state management via `StateFlow<GameState>`.
- `GameRenderer.kt` — all Canvas drawing via `SurfaceHolder.lockHardwareCanvas()` for GPU acceleration.
- `NoteManager.kt` — note scheduling and hit detection.
- `ScoreManager.kt` — scoring, combo, overpress tracking.
- `AudioSyncManager.kt` — ExoPlayer wrapper. Position is read ~250 Hz on a background thread and cached in an `AtomicLong` so the render thread never blocks on ExoPlayer.
- `InputHandler.kt` — maps touch coordinates to lane numbers.

**Critical rendering rules** (any new drawing code must follow these to avoid destroying frame rate):
- All `Paint` objects are pre-allocated at init. Never `new Paint()` per frame.
- Pre-cached shader arrays (`laneNoteShaders`, `fretSocketShaders`, `fretGlowShaders`, `fretPressedGlowShaders`) are **repositioned via `Matrix.setLocalMatrix()`** — never create a new `RadialGradient` / `LinearGradient` per frame.
- **No `BlurMaskFilter` or `Paint.setShadowLayer()`** — both force software-rendering fallback.
- Pre-allocated `Path` and `RectF` objects (`highwayPath`, `laneHighlightPath`, `holdTrailPath`, `rectF`, `shaderMatrix`) are reused each frame.

**Highway geometry** (in `GameRenderer.kt`, all relative to `screenWidth`/`screenHeight` so they stay correct across devices):
- `highwayTopY = 0.24 * screenHeight`, `highwayBottomY = hitLineY = 0.92 * screenHeight` — gives ~68% vertical span with the bulk of empty space above (for score HUD) rather than below.
- `highwayBottomWidth = 0.58 * screenWidth`, `highwayTopWidth = 0.05 * screenWidth` — narrow highway leaves ~21% of screen width free on each side for HUD elements (power bar, score, combo). This was tuned for landscape; portrait would want different values.
- `fretButtonRadius = minOf(highwayBottomWidth / laneCount / 2 * 0.62, screenHeight * 0.055)` — capping by screen height prevents oversized circles on wide landscape screens. Note/hold-trail/hit-effect sizes all derive from `fretButtonRadius` so tuning this one value scales everything proportionally.
- `perspectiveExponent = 2.5f` controls the steepness of the fake-3D curve in `perspectiveY()` / `perspectiveScale()`.

### UI Theme — "Guitar Hero / Stage Rock"
- Design tokens centralized in `ui/theme/DesignTokens.kt`. The `DesignTokens.Stage` object holds the stage colors (`FireOrange`, `FireYellow`, `FlameRed`, `StageGold`, `DarkChrome`, `SteelGray`). The older `DesignTokens.Neon` object is kept because `GameRenderer` hardcodes lane colors against it.
- Themed component library in `ui/components/StageComponents.kt` and `ui/components/CosmicComponents.kt`: `NebulaMenuBackground`, `MusicalNotesBackground`, `StageBackground`, `ThemedButton`, `ThemedWaveformBars`, `CurrencyBar`, `UserProfileHeader`, `GalaxySwirl`, `FlameEffect`, and `GuitarButton`.
- **`GuitarButton`** is the main-menu button pattern: it displays a pre-rendered PNG drawable (`res/drawable/btn_oyna.png`, `btn_sarki_ekle.png`, `btn_multi.png`, `btn_ranked.png`, `btn_magaza.png`, `btn_profil.png`) at `fillMaxWidth` with `ContentScale.FillWidth` and `alpha = 0.70f`. Text and icons are baked into the PNGs — do not overlay extra decoration. All PNGs were split from a single composite via alpha-bbox detection and keep their natural aspect ratios.
- Fonts: `SpaceGroteskFontFamily` (display/headlines), `ManropeFontFamily` (body/labels).
- `beveledSurface()` modifier provides the beveled chrome look on buttons/cards.

### Data Flow
1. Upload: Android `UploadScreen` → `POST /upload` → backend saves audio and starts background analysis.
2. Poll: Android → `GET /status/{songId}` until status becomes `ready` (`processing`/`analyzing`/`converting` are the transitional states).
3. Play: Android → `GET /chart/{songId}?difficulty=medium` → returns a JSON note array.
4. Game: `GameEngine` schedules notes, `GameRenderer` draws on the dedicated thread, `ScoreManager` tracks hits.
5. Result: Score/combo/accuracy displayed on `ResultScreen` and saved via Room (`SongRepository.updateHighScore`).

## Game Design Constants
- **5 lanes** mapped by log-frequency bands: 80–200, 200–400, 400–800, 800–1600, 1600–5000 Hz.
- **Hit windows**: Perfect ±100 ms, Great ±200 ms, Good ±300 ms.
- **Combo multiplier**: +0.1× per 10 hits, capped at 4.0×.
- **Power bar**: double-tap to activate at ≥50 combo (2× for 10 s) or ≥100 combo (4× for 20 s).
- **Overpress**: tapping a lane with no note breaks combo.
- **Difficulties**: Easy (3 lanes, 40% notes reduced), Medium (5 lanes, 20% reduced), Hard (all notes + chords).
- **Grades**: S ≥95%, A ≥85%, B ≥75%, C ≥65%, D <65% accuracy.

## Key Conventions
- All UI colors come from `DesignTokens` — never hardcoded in screens or components.
- The result-screen nav route (`NavRoutes.result(...)`) carries every stat as a URL param: `songId, difficulty, score, maxCombo, perfect, great, good, miss, overpress`. Any change to game stats means updating both `NavRoutes.result()` and the `GameScreen.onGameFinished` 7-param callback.
- `GameState` is a `data class` flowing via `StateFlow` from `GameEngine` to Compose. Never mutate it from the UI — the engine is the sole writer.
- When adding new drawing to `GameRenderer`, follow the "no per-frame allocation" rules above and derive sizes from `fretButtonRadius` / `screenHeight` so landscape scaling stays correct.
- Landscape-only: do not reintroduce `verticalScroll` on primary screens (`HomeScreen`, `SongDetailScreen`). Fit content via 2-column layouts and tightened typography/heights.
