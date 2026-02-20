package com.rhythmgame.game

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.rhythmgame.data.model.Chart
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
    private val onGameFinished: (GameState) -> Unit = {},
) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: Job? = null
    private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private lateinit var renderer: GameRenderer
    private lateinit var noteManager: NoteManager
    private lateinit var inputHandler: InputHandler
    private val scoreManager = ScoreManager()

    private val _gameState = MutableStateFlow(GameState(phase = GamePhase.LOADING))
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var gameStartTimeMs = 0L
    private var totalPausedMs = 0L
    private var pauseStartMs = 0L
    private val hitEffects = mutableListOf<HitEffect>()
    private val pressedLanes = mutableSetOf<Int>()
    private val particles = mutableListOf<Particle>()
    private val random = Random()
    private val lock = Any()  // Synchronizes access to hitEffects, particles, pressedLanes

    // Power Bar state
    private var powerActive = false
    private var powerStartTimeMs = 0L
    private var powerDurationMs = 0L
    private var powerMultiplier = 1f
    private var powerActivationFill = 0f
    private var lastPowerTapTimeMs = 0L
    private val doubleTapThresholdMs = 400L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val w = width.toFloat()
        val h = height.toFloat()

        renderer = GameRenderer(w, h)
        noteManager = NoteManager(
            notes = chart.notes,
            hitLineY = renderer.getHitLineY(),
            screenHeight = h,
        )
        inputHandler = InputHandler(
            screenWidth = w,
            highwayBottomLeft = renderer.getHighwayBottomLeft(),
            highwayBottomWidth = renderer.getHighwayBottomWidth(),
        )

        startCountdown()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopGame()
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
                _gameState.value = _gameState.value.copy(countdownValue = i)
                renderFrame()
                delay(1000)
            }
            startGame()
        }
    }

    private fun startGame() {
        _gameState.value = _gameState.value.copy(phase = GamePhase.PLAYING)
        gameStartTimeMs = System.currentTimeMillis()

        val launchGameLoop = {
            isRunning = true
            gameThread = scope.launch {
                val targetFrameTimeMs = 16L  // ~60 FPS
                while (isRunning && _gameState.value.phase == GamePhase.PLAYING) {
                    val frameStart = System.currentTimeMillis()
                    updateGame()
                    renderFrame()
                    val elapsed = System.currentTimeMillis() - frameStart
                    val sleepTime = targetFrameTimeMs - elapsed
                    if (sleepTime > 0) delay(sleepTime)
                }
            }
        }

        if (audioSyncManager != null) {
            // Wait for audio to be ready before starting game loop
            audioSyncManager.play {
                gameStartTimeMs = System.currentTimeMillis()
                launchGameLoop()
            }
        } else {
            launchGameLoop()
        }
    }

    private fun updateGame() {
        val currentTimeMs = if (audioSyncManager != null) {
            audioSyncManager.getCurrentPositionMs()
        } else {
            System.currentTimeMillis() - gameStartTimeMs
        }

        val activeNotes = noteManager.update(currentTimeMs)

        synchronized(lock) {
            // Check for missed notes
            for (note in activeNotes) {
                if (note.hitResult == HitResult.MISS && note.isHit && !note.missReported) {
                    note.missReported = true
                    scoreManager.onHit(HitResult.MISS)
                    hitEffects.add(
                        HitEffect(note.note.lane, HitResult.MISS, System.currentTimeMillis(), renderer.getHitLineY())
                    )
                }
            }

            // Clean old hit effects
            val now = System.currentTimeMillis()
            hitEffects.removeAll { now - it.createdAt > 600 }

            // Update particles
            val particleIterator = particles.iterator()
            while (particleIterator.hasNext()) {
                val p = particleIterator.next()
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.3f // gravity
                p.life -= 0.02f
                p.size *= 0.98f
                if (p.life <= 0f) particleIterator.remove()
            }
        }

        // Check if game is over: 1.5 seconds after song duration (wall clock, guaranteed)
        val elapsedSinceStart = System.currentTimeMillis() - gameStartTimeMs - totalPausedMs
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
                val elapsedPower = System.currentTimeMillis() - powerStartTimeMs
                val remaining = (powerDurationMs - elapsedPower).coerceAtLeast(0)
                pbProgress = (remaining.toFloat() / powerDurationMs) * powerActivationFill
                pbRemainingMs = remaining

                if (remaining <= 0) {
                    powerActive = false
                    powerMultiplier = 1f
                    scoreManager.setPowerMultiplier(1f)
                }
                pbActive = powerActive
                pbMultiplier = powerMultiplier
            } else {
                pbProgress = (scoreManager.getCombo().toFloat() / 100f).coerceAtMost(1f)
                pbRemainingMs = 0
                pbActive = false
                pbMultiplier = 1f
            }

            _gameState.value = _gameState.value.copy(
                currentTimeMs = currentTimeMs,
                score = scoreManager.getScore(),
                combo = scoreManager.getCombo(),
                maxCombo = scoreManager.getMaxCombo(),
                perfectCount = scoreManager.getPerfectCount(),
                greatCount = scoreManager.getGreatCount(),
                goodCount = scoreManager.getGoodCount(),
                missCount = scoreManager.getMissCount(),
                comboMultiplier = if (pbActive) pbMultiplier else scoreManager.comboMultiplier.coerceAtMost(4.0f),
                activeNotes = activeNotes,
                hitEffects = hitEffects.toList(),
                pressedLanes = pressedLanes.toSet(),
                particles = particles.toList(),
                songDurationMs = chart.durationMs.toLong(),
                powerBarProgress = pbProgress,
                powerActive = pbActive,
                powerMultiplier = pbMultiplier,
                powerRemainingMs = pbRemainingMs,
                powerDurationMs = this@GameEngine.powerDurationMs,
            )
        }
    }

    private fun renderFrame() {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                synchronized(holder) {
                    renderer.render(canvas, _gameState.value)
                }
            }
        } finally {
            if (canvas != null) {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val state = _gameState.value

        if (state.phase == GamePhase.PAUSED) {
            return true // Consume touch but don't resume - handled by Compose overlay
        }

        if (state.phase != GamePhase.PLAYING) return true

        val currentTimeMs = if (audioSyncManager != null) {
            audioSyncManager.getCurrentPositionMs()
        } else {
            System.currentTimeMillis() - gameStartTimeMs
        }

        // Power bar double-tap detection (right side of screen)
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val pointerIndex = event.actionIndex
            val tapX = event.getX(pointerIndex)
            val tapY = event.getY(pointerIndex)
            if (isInPowerBarZone(tapX, tapY)) {
                handlePowerBarTap()
                return true
            }
        }

        val touchEvents = inputHandler.processMotionEvent(event)
        synchronized(lock) {
            for (touchEvent in touchEvents) {
                when (touchEvent.action) {
                    InputHandler.TouchAction.DOWN -> {
                        pressedLanes.add(touchEvent.lane)
                        val result = noteManager.tryHit(touchEvent.lane, currentTimeMs)
                        if (result != null) {
                            val (hitResult, _) = result
                            scoreManager.onHit(hitResult)
                            spawnParticles(touchEvent.lane, hitResult)
                            hitEffects.add(
                                HitEffect(touchEvent.lane, hitResult, System.currentTimeMillis(), renderer.getHitLineY())
                            )
                        }
                    }
                    InputHandler.TouchAction.UP -> {
                        pressedLanes.remove(touchEvent.lane)
                        noteManager.releaseHold(touchEvent.lane)
                    }
                }
            }
        }

        return true
    }

    private fun spawnParticles(lane: Int, result: HitResult) {
        if (result == HitResult.MISS) return
        val centerX = renderer.getLaneCenterXAtStrikeline(lane - 1)
        val centerY = renderer.getStrikeLineY()
        val color = when (lane) {
            1 -> android.graphics.Color.rgb(76, 175, 80)
            2 -> android.graphics.Color.rgb(255, 87, 34)
            3 -> android.graphics.Color.rgb(255, 235, 59)
            4 -> android.graphics.Color.rgb(33, 150, 243)
            5 -> android.graphics.Color.rgb(255, 152, 0)
            else -> android.graphics.Color.WHITE
        }
        val count = when (result) {
            HitResult.PERFECT -> 25
            HitResult.GREAT -> 18
            HitResult.GOOD -> 12
            else -> 0
        }
        for (i in 0 until count) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2
            val speed = 3f + random.nextFloat() * 8f
            particles.add(Particle(
                x = centerX,
                y = centerY,
                vx = kotlin.math.cos(angle) * speed,
                vy = -kotlin.math.abs(kotlin.math.sin(angle) * speed) - 2f,
                life = 1.0f,
                color = color,
                size = 4f + random.nextFloat() * 6f,
            ))
        }
    }

    private fun isInPowerBarZone(x: Float, y: Float): Boolean {
        return x > width * 0.92f && y > height * 0.2f && y < height * 0.78f
    }

    private fun handlePowerBarTap() {
        val now = System.currentTimeMillis()
        if (now - lastPowerTapTimeMs < doubleTapThresholdMs) {
            activatePower()
            lastPowerTapTimeMs = 0L
        } else {
            lastPowerTapTimeMs = now
        }
    }

    private fun activatePower() {
        if (powerActive) return
        val combo = scoreManager.getCombo()
        val tier = when {
            combo >= 100 -> 2
            combo >= 50 -> 1
            else -> return
        }

        powerActive = true
        powerStartTimeMs = System.currentTimeMillis()
        powerActivationFill = (combo.toFloat() / 100f).coerceAtMost(1f)

        when (tier) {
            2 -> { powerDurationMs = 20_000L; powerMultiplier = 4f }
            else -> { powerDurationMs = 10_000L; powerMultiplier = 2f }
        }
        scoreManager.setPowerMultiplier(powerMultiplier)
    }

    fun pauseGame() {
        if (_gameState.value.phase == GamePhase.PLAYING) {
            pauseStartMs = System.currentTimeMillis()
            _gameState.value = _gameState.value.copy(phase = GamePhase.PAUSED)
            audioSyncManager?.pause()
            isRunning = false
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
            _gameState.value = _gameState.value.copy(phase = GamePhase.PLAYING)

            val launchGameLoop = {
                isRunning = true
                gameThread = scope.launch {
                    val targetFrameTimeMs = 16L
                    while (isRunning && _gameState.value.phase == GamePhase.PLAYING) {
                        val frameStart = System.currentTimeMillis()
                        updateGame()
                        renderFrame()
                        val elapsed = System.currentTimeMillis() - frameStart
                        val sleepTime = targetFrameTimeMs - elapsed
                        if (sleepTime > 0) delay(sleepTime)
                    }
                }
            }

            if (audioSyncManager != null) {
                audioSyncManager.play { launchGameLoop() }
            } else {
                launchGameLoop()
            }
        }
    }

    private fun finishGame() {
        isRunning = false
        audioSyncManager?.stop()
        val finalState = _gameState.value.copy(phase = GamePhase.FINISHED)
        _gameState.value = finalState
        renderFrame()
        Handler(Looper.getMainLooper()).post {
            onGameFinished(finalState)
        }
    }

    fun stopGame() {
        isRunning = false
        gameThread?.cancel()
        audioSyncManager?.release()
        scope.cancel()
    }
}
