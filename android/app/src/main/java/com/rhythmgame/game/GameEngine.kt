package com.rhythmgame.game

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.rhythmgame.R
import com.rhythmgame.data.model.Chart
import com.rhythmgame.util.VibrationManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random

class GameEngine(
    context: Context,
    private val chart: Chart,
    private val audioSyncManager: AudioSyncManager?,
    private val audioOffsetMs: Long = 0,
    private val gameScreenStyle: String = "highway",
    private val particlesEnabled: Boolean = true,
    private val vibrationManager: VibrationManager? = null,
    private val onGameFinished: (GameState) -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback {

    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var renderer: IGameRenderer
    private lateinit var noteManager: NoteManager
    private lateinit var inputHandler: InputHandler
    private val scoreManager = ScoreManager()

    private val _gameState = MutableStateFlow(GameState(phase = GamePhase.LOADING, particlesEnabled = particlesEnabled))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var gameStartTimeMs = 0L
    private var totalPausedMs = 0L
    private var pauseStartMs = 0L
    private val hitEffects = mutableListOf<HitEffect>()
    private val pressedLanes = mutableSetOf<Int>()
    private val particles = mutableListOf<Particle>()
    private val particlePool = ParticlePool(300)
    private val random = Random()
    private val lock = Any()
    private val renderLock = Any()

    // Pointer-to-lane tracking for glissando
    private val pointerLanes = mutableMapOf<Int, Int>()

    // Smoke particle constants
    private val maxParticles = 250
    private val smokeVariantCount = 4
    private val maxSmokeSize = 45f

    // Power Bar state
    private var powerActive = false
    private var powerStartTimeMs = 0L
    private var powerDurationMs = 0L
    private var powerMultiplier = 1f
    private var powerActivationFill = 0f
    private var powerBarCombo = 0  // Separate combo counter for power bar (not incremented during special)

    // Shared power bar geometry
    private var powerBarGeometry: PowerBarGeometry? = null

    // Video backgrounds (normal + power/special mode)
    private var videoBackgroundNormal: VideoBackgroundPlayer? = null
    private var videoBackgroundPower: VideoBackgroundPlayer? = null

    // Surface lifecycle: true when surface was lost while game was in progress
    private var surfaceLost = false

    // Track whether UI state needs updating (reduces GameState copies)
    @Volatile
    private var stateNeedsUpdate = true

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val w = width.toFloat()
        val h = height.toFloat()

        val isArc = gameScreenStyle == "arc"
        val bpm = chart.bpm.coerceIn(40f, 300f)
        val approachTimeMs = (120f / bpm * 1800f).toLong().coerceIn(1200L, 2800L)

        // Load guitar power bar bitmaps
        val guitarBitmap = try { BitmapFactory.decodeResource(resources, R.drawable.guitar_power_empty) } catch (_: Exception) { null }
        val guitarFullBitmap = try { BitmapFactory.decodeResource(resources, R.drawable.guitar_power_full) } catch (_: Exception) { null }

        powerBarGeometry = PowerBarGeometry(w, h, isArc)
        renderer = if (isArc) ArcGameRenderer(w, h, approachTimeMs, guitarBitmap, guitarFullBitmap) else GameRenderer(w, h, approachTimeMs, guitarBitmap, guitarFullBitmap)

        // Start or reuse video background decoders
        val vw = (w / 4).toInt().coerceAtLeast(120)
        val vh = (h / 4).toInt().coerceAtLeast(68)
        if (videoBackgroundNormal == null) {
            try {
                videoBackgroundNormal = VideoBackgroundPlayer(context, "bg_video.mp4", vw, vh)
            } catch (e: Exception) {
                android.util.Log.w("GameEngine", "Normal video bg not available", e)
            }
        }
        if (videoBackgroundPower == null) {
            try {
                videoBackgroundPower = VideoBackgroundPlayer(context, "bg_video_power.mp4", vw, vh)
            } catch (e: Exception) {
                android.util.Log.w("GameEngine", "Power video bg not available", e)
            }
        }
        renderer.setVideoBackground(videoBackgroundNormal, videoBackgroundPower)

        if (surfaceLost) {
            // Returning from background / screen lock — renderer was recreated, game state preserved
            surfaceLost = false
            inputHandler = if (isArc) {
                InputHandler(
                    screenWidth = w,
                    highwayBottomLeft = renderer.getHighwayBottomLeft(),
                    highwayBottomWidth = renderer.getHighwayBottomWidth(),
                    arcRenderer = renderer as ArcGameRenderer,
                )
            } else {
                InputHandler(
                    screenWidth = w,
                    highwayBottomLeft = renderer.getHighwayBottomLeft(),
                    highwayBottomWidth = renderer.getHighwayBottomWidth(),
                )
            }
            // Render current paused frame so screen isn't blank
            renderFrame()
            return
        }

        // First-time initialization
        noteManager = NoteManager(
            notes = chart.notes,
            hitLineY = renderer.getHitLineY(),
            screenHeight = h,
            approachTimeMs = approachTimeMs,
        )
        inputHandler = if (isArc) {
            InputHandler(
                screenWidth = w,
                highwayBottomLeft = renderer.getHighwayBottomLeft(),
                highwayBottomWidth = renderer.getHighwayBottomWidth(),
                arcRenderer = renderer as ArcGameRenderer,
            )
        } else {
            InputHandler(
                screenWidth = w,
                highwayBottomLeft = renderer.getHighwayBottomLeft(),
                highwayBottomWidth = renderer.getHighwayBottomWidth(),
            )
        }

        startCountdown()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val phase = _gameState.value.phase
        if (phase == GamePhase.FINISHED) {
            stopGame()
            return
        }
        // Auto-pause when surface is lost (backgrounding, screen lock)
        if (phase == GamePhase.PLAYING || phase == GamePhase.COUNTDOWN || phase == GamePhase.RESUME_COUNTDOWN) {
            pauseGame()
        }
        surfaceLost = true
    }

    private fun startCountdown() {
        _gameState.value = _gameState.value.copy(
            phase = GamePhase.COUNTDOWN,
            countdownValue = 3,
            totalNotes = noteManager.getTotalNotes(),
            songDurationMs = chart.durationMs.toLong(),
        )

        scope.launch {
            for (i in 3 downTo 1) {
                _gameState.value = _gameState.value.copy(
                    countdownValue = i,
                    frameTimeMs = System.currentTimeMillis(),
                )
                renderFrame()
                delay(1000)
            }
            startGame()
        }
    }

    private var gameThread: Thread? = null

    private fun startGameLoop() {
        isRunning = true
        gameThread = Thread({
            var lastTimeNanos = System.nanoTime()
            while (isRunning) {
                if (_gameState.value.phase != GamePhase.PLAYING) {
                    lastTimeNanos = System.nanoTime()
                    try { Thread.sleep(16) } catch (_: InterruptedException) { break }
                    continue
                }
                val now = System.nanoTime()
                val deltaTimeS = (now - lastTimeNanos).toFloat() / 1_000_000_000f
                lastTimeNanos = now
                updateGame(deltaTimeS)
                renderFrame()
            }
        }, "GameLoop").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun startGame() {
        _gameState.value = _gameState.value.copy(phase = GamePhase.PLAYING)
        gameStartTimeMs = System.currentTimeMillis()

        if (audioSyncManager != null) {
            audioSyncManager.play {
                gameStartTimeMs = System.currentTimeMillis()
                startGameLoop()
            }
        } else {
            startGameLoop()
        }
    }

    private fun updateGame(deltaTimeS: Float) {
        // Single timestamp for the entire frame
        val now = System.currentTimeMillis()

        val currentTimeMs = if (audioSyncManager != null) {
            audioSyncManager.getCurrentPositionMs()
        } else {
            now - gameStartTimeMs
        }

        val activeNotes = noteManager.update(currentTimeMs)
        val autoCompletedHolds = noteManager.updateHolds(currentTimeMs)

        synchronized(lock) {
            // Score auto-completed hold notes (held for full duration)
            for ((hitResult, completedNote) in autoCompletedHolds) {
                scoreManager.onHit(hitResult)
                if (hitResult != HitResult.MISS) {
                    powerBarCombo++
                } else {
                    powerBarCombo = 0
                }
                hitEffects.add(
                    HitEffect.create(completedNote.note.lane, hitResult, now, renderer.getHitLineY())
                )
                stateNeedsUpdate = true
            }

            // Check for missed notes
            for (note in activeNotes) {
                if (note.hitResult == HitResult.MISS && note.isHit && !note.missReported) {
                    note.missReported = true
                    scoreManager.onHit(HitResult.MISS)
                    powerBarCombo = 0
                    hitEffects.add(
                        HitEffect.create(note.note.lane, HitResult.MISS, now, renderer.getHitLineY())
                    )
                    stateNeedsUpdate = true
                }
            }

            // Clean old hit effects
            hitEffects.removeAll { now - it.createdAt > 600 }

            // Update smoke particles: air resistance, expansion, no gravity
            val frames = deltaTimeS * 60f
            val drag = (1f - 0.02f * frames).coerceIn(0.5f, 1f)
            val expand = 1f + 0.02f * frames

            val particleIterator = particles.iterator()
            while (particleIterator.hasNext()) {
                val p = particleIterator.next()
                p.x += p.vx * frames
                p.y += p.vy * frames
                p.vx *= drag
                p.vy *= drag
                p.size = (p.size * expand).coerceAtMost(maxSmokeSize)
                p.rotation += p.vx * 2f * frames
                p.life -= 0.02f * frames
                if (p.life <= 0f) {
                    particleIterator.remove()
                    particlePool.recycle(p)
                }
            }

            // Continuous smoke emission for held lanes (1-2 particles/frame)
            if (particlesEnabled) {
                for (lane in pressedLanes) {
                    if (particles.size >= maxParticles) break
                    spawnSmokeParticle(lane)
                    if (random.nextFloat() < 0.5f && particles.size < maxParticles) {
                        spawnSmokeParticle(lane)
                    }
                }
            }
        }

        // Check if game is over
        val elapsedSinceStart = now - gameStartTimeMs - totalPausedMs
        if (elapsedSinceStart > chart.durationMs + 1500) {
            finishGame()
            return
        }

        synchronized(lock) {
            // Update power bar state
            val pbProgress: Float
            val pbRemainingMs: Long
            val pbActive: Boolean
            val pbMultiplier: Float

            if (powerActive) {
                val elapsedPower = now - powerStartTimeMs
                val remaining = (powerDurationMs - elapsedPower).coerceAtLeast(0)
                pbProgress = (remaining.toFloat() / powerDurationMs) * powerActivationFill
                pbRemainingMs = remaining

                if (remaining <= 0) {
                    powerActive = false
                    powerMultiplier = 1f
                    powerBarCombo = 0
                    scoreManager.setPowerMultiplier(1f)
                }
                pbActive = powerActive
                pbMultiplier = powerMultiplier
            } else {
                pbProgress = (powerBarCombo.toFloat() / 100f).coerceAtMost(1f)
                pbRemainingMs = 0
                pbActive = false
                pbMultiplier = 1f
            }

            _gameState.value = _gameState.value.copy(
                currentTimeMs = currentTimeMs,
                frameTimeMs = now,
                score = scoreManager.getScore(),
                combo = scoreManager.getCombo(),
                maxCombo = scoreManager.getMaxCombo(),
                perfectCount = scoreManager.getPerfectCount(),
                greatCount = scoreManager.getGreatCount(),
                goodCount = scoreManager.getGoodCount(),
                missCount = scoreManager.getMissCount(),
                overpressCount = scoreManager.getOverpressCount(),
                comboMultiplier = scoreManager.totalMultiplier,
                activeNotes = activeNotes,
                hitEffects = hitEffects.toList(),
                pressedLanes = pressedLanes.toSet(),
                particles = particles.toList(),
                songDurationMs = chart.durationMs.toLong(),
                powerBarProgress = pbProgress,
                powerBarCombo = powerBarCombo,
                powerActive = pbActive,
                powerMultiplier = pbMultiplier,
                powerRemainingMs = pbRemainingMs,
                powerDurationMs = this@GameEngine.powerDurationMs,
            )
        }
    }

    private fun renderFrame() {
        synchronized(renderLock) {
            val canvas: Canvas = try {
                holder.lockHardwareCanvas()
            } catch (_: Exception) {
                try { holder.lockCanvas() } catch (_: Exception) { null }
            } ?: return
            try {
                renderer.render(canvas, _gameState.value)
            } finally {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val state = _gameState.value

        if (state.phase == GamePhase.PAUSED) return true
        if (state.phase != GamePhase.PLAYING) return true

        val now = System.currentTimeMillis()
        val currentTimeMs = if (audioSyncManager != null) {
            audioSyncManager.getCurrentPositionMs()
        } else {
            now - gameStartTimeMs
        }

        // Guitar power bar tap detection — ignore this pointer for lane input
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val pointerIndex = event.actionIndex
            val pointerId = event.getPointerId(pointerIndex)
            val tapX = event.getX(pointerIndex)
            val tapY = event.getY(pointerIndex)
            if (isInPowerBarZone(tapX, tapY)) {
                inputHandler.ignorePointer(pointerId)
                handlePowerBarTap()
                return true
            }
        }

        val touchEvents = inputHandler.processMotionEvent(event)
        synchronized(lock) {
            for (touchEvent in touchEvents) {
                when (touchEvent.action) {
                    InputHandler.TouchAction.DOWN -> {
                        pointerLanes[touchEvent.pointerId] = touchEvent.lane
                        val result = noteManager.tryHit(touchEvent.lane, currentTimeMs)
                        if (result != null) {
                            val (hitResult, activeNote) = result
                            vibrationManager?.vibrateOnHit(hitResult)
                            val isHoldStart = activeNote?.note?.isHold == true
                            // For hold notes, don't score on initial tap — score on release
                            if (!isHoldStart) {
                                scoreManager.onHit(hitResult)
                                if (hitResult != HitResult.MISS) {
                                    powerBarCombo++
                                } else {
                                    powerBarCombo = 0
                                }
                            }
                            hitEffects.add(
                                HitEffect.create(touchEvent.lane, hitResult, now, renderer.getHitLineY())
                            )
                        } else {
                            // Overpress: tapped lane with no note nearby
                            scoreManager.onOverpress()
                            powerBarCombo = 0
                            hitEffects.add(
                                HitEffect.create(touchEvent.lane, HitResult.MISS, now, renderer.getHitLineY())
                            )
                        }
                        // Smoke burst on tap/glissando enter (3 particles)
                        if (particlesEnabled) spawnSmokeBurst(touchEvent.lane)
                        stateNeedsUpdate = true
                    }
                    InputHandler.TouchAction.UP -> {
                        pointerLanes.remove(touchEvent.pointerId)
                        val holdResult = noteManager.releaseHold(touchEvent.lane)
                        if (holdResult != null) {
                            val (hitResult, _) = holdResult
                            scoreManager.onHit(hitResult)
                            if (hitResult != HitResult.MISS) {
                                powerBarCombo++
                            } else {
                                powerBarCombo = 0
                            }
                            hitEffects.add(
                                HitEffect.create(touchEvent.lane, hitResult, now, renderer.getHitLineY())
                            )
                            stateNeedsUpdate = true
                        }
                    }
                }
            }
            // Recompute pressedLanes from active pointer mapping
            pressedLanes.clear()
            pressedLanes.addAll(pointerLanes.values)
        }

        return true
    }

    /**
     * Spawn a burst of 3 smoke particles on initial tap or glissando lane-enter.
     * Must be called while holding [lock].
     */
    private fun spawnSmokeBurst(lane: Int) {
        val laneIndex = lane - 1
        val (centerX, centerY) = renderer.getFretPosition(laneIndex)
        val (dirX, dirY) = renderer.getParticleDirection(laneIndex)
        val color = renderer.getLaneColor(laneIndex)

        for (i in 0 until 3) {
            if (particles.size >= maxParticles) break
            val speed = 4f + random.nextFloat() * 4f
            particles.add(particlePool.obtain(
                x = centerX + (random.nextFloat() - 0.5f) * 20f,
                y = centerY + dirY * random.nextFloat() * 10f,
                vx = dirX * speed + (random.nextFloat() - 0.5f) * 2f,
                vy = dirY * speed + (random.nextFloat() - 0.5f) * 2f,
                life = 1.0f,
                color = color,
                size = 10f + random.nextFloat() * 8f,
                rotation = random.nextFloat() * 360f,
                textureIndex = random.nextInt(smokeVariantCount),
                laneIndex = laneIndex,
            ))
        }
    }

    /**
     * Spawn a single smoke particle for continuous emission while a lane is held.
     * Must be called while holding [lock].
     */
    private fun spawnSmokeParticle(lane: Int) {
        val laneIndex = lane - 1
        val (centerX, centerY) = renderer.getFretPosition(laneIndex)
        val (dirX, dirY) = renderer.getParticleDirection(laneIndex)
        val color = renderer.getLaneColor(laneIndex)

        val speed = 3f + random.nextFloat() * 3f
        particles.add(particlePool.obtain(
            x = centerX + (random.nextFloat() - 0.5f) * 16f,
            y = centerY + dirY * random.nextFloat() * 8f,
            vx = dirX * speed + (random.nextFloat() - 0.5f) * 1.5f,
            vy = dirY * speed + (random.nextFloat() - 0.5f) * 1.5f,
            life = 1.0f,
            color = color,
            size = 8f + random.nextFloat() * 6f,
            rotation = random.nextFloat() * 360f,
            textureIndex = random.nextInt(smokeVariantCount),
            laneIndex = laneIndex,
        ))
    }

    private fun isInPowerBarZone(x: Float, y: Float): Boolean {
        return powerBarGeometry?.isInZone(x, y) ?: false
    }

    private fun handlePowerBarTap() {
        // Single tap activation (no double-tap needed)
        activatePower()
    }

    private fun activatePower() {
        if (powerActive) return
        val pbCombo = powerBarCombo
        val tier = when {
            pbCombo >= 100 -> 2
            pbCombo >= 50 -> 1
            else -> return
        }

        powerActive = true
        powerStartTimeMs = System.currentTimeMillis()
        powerActivationFill = (pbCombo.toFloat() / 100f).coerceAtMost(1f)
        // Reset combo counter but allow it to keep incrementing during power
        powerBarCombo = 0

        when (tier) {
            2 -> { powerDurationMs = 20_000L; powerMultiplier = 4f }
            else -> { powerDurationMs = 10_000L; powerMultiplier = 2f }
        }
        scoreManager.setPowerMultiplier(powerMultiplier)
    }

    fun pauseGame() {
        val phase = _gameState.value.phase
        if (phase == GamePhase.PLAYING || phase == GamePhase.COUNTDOWN || phase == GamePhase.RESUME_COUNTDOWN) {
            pauseStartMs = System.currentTimeMillis()
            _gameState.value = _gameState.value.copy(
                phase = GamePhase.PAUSED,
                frameTimeMs = pauseStartMs,
            )
            audioSyncManager?.pause()
            // Score active holds before clearing touch state
            synchronized(lock) {
                val holdResults = noteManager.finalizeActiveHolds()
                for ((hitResult, _) in holdResults) {
                    scoreManager.onHit(hitResult)
                    if (hitResult != HitResult.MISS) {
                        powerBarCombo++
                    } else {
                        powerBarCombo = 0
                    }
                }
                pressedLanes.clear()
                pointerLanes.clear()
            }
            inputHandler.reset()
            renderFrame()
        }
    }

    fun resumeGame() {
        if (_gameState.value.phase == GamePhase.PAUSED) {
            val pauseDuration = System.currentTimeMillis() - pauseStartMs
            totalPausedMs += pauseDuration
            if (powerActive) {
                powerStartTimeMs += pauseDuration
            }
            startResumeCountdown()
        }
    }

    private fun startResumeCountdown() {
        _gameState.value = _gameState.value.copy(
            phase = GamePhase.RESUME_COUNTDOWN,
            countdownValue = 3,
        )
        scope.launch {
            for (i in 3 downTo 1) {
                _gameState.value = _gameState.value.copy(
                    countdownValue = i,
                    frameTimeMs = System.currentTimeMillis(),
                )
                renderFrame()
                delay(1000)
                // If user paused again during countdown, stop
                if (_gameState.value.phase != GamePhase.RESUME_COUNTDOWN) return@launch
            }
            _gameState.value = _gameState.value.copy(phase = GamePhase.PLAYING)
            audioSyncManager?.resume()
        }
    }

    private fun finishGame() {
        isRunning = false
        audioSyncManager?.stop()

        // Score any hold notes that are still actively held
        synchronized(lock) {
            val holdResults = noteManager.finalizeActiveHolds()
            for ((hitResult, _) in holdResults) {
                scoreManager.onHit(hitResult)
                if (hitResult != HitResult.MISS) {
                    powerBarCombo++
                } else {
                    powerBarCombo = 0
                }
            }
            pressedLanes.clear()
            pointerLanes.clear()
        }

        val finalState = _gameState.value.copy(
            phase = GamePhase.FINISHED,
            score = scoreManager.getScore(),
            maxCombo = scoreManager.getMaxCombo(),
            perfectCount = scoreManager.getPerfectCount(),
            greatCount = scoreManager.getGreatCount(),
            goodCount = scoreManager.getGoodCount(),
            missCount = scoreManager.getMissCount(),
            overpressCount = scoreManager.getOverpressCount(),
        )
        _gameState.value = finalState
        renderFrame()
        Handler(Looper.getMainLooper()).post {
            onGameFinished(finalState)
        }
    }

    fun stopGame() {
        isRunning = false
        gameThread?.interrupt()
        try { gameThread?.join(500) } catch (_: Exception) {}
        gameThread = null
        audioSyncManager?.release()
        videoBackgroundNormal?.release()
        videoBackgroundPower?.release()
        videoBackgroundNormal = null
        videoBackgroundPower = null
        scope.cancel()
    }
}
