package com.rhythmgame.game

import android.graphics.*
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(
    private val screenWidth: Float,
    private val screenHeight: Float,
    approachTimeMs: Long = 2000L,
    private val guitarBitmap: Bitmap? = null,
    private val guitarFullBitmap: Bitmap? = null,
) : IGameRenderer {
    private val approachTimeMsF = approachTimeMs.toFloat()
    private val laneCount = 5

    // --- Highway Geometry (3D perspective) ---
    // Shift the whole highway down: less empty space below the strikeline.
    // Both top and bottom shifted by the same 0.12 so the vertical span
    // (0.68 * screenHeight) and perspective curve remain unchanged.
    private val hitLineY = screenHeight * 0.92f
    private val highwayBottomY = hitLineY
    private val highwayTopY = screenHeight * 0.24f
    // Narrower highway so lanes sit closer together and leave room on the
    // sides for HUD (score/power bar/combo) in landscape.
    private val highwayBottomWidth = screenWidth * 0.58f
    private val highwayTopWidth = screenWidth * 0.05f
    private val highwayBottomLeft = (screenWidth - highwayBottomWidth) / 2f
    private val highwayBottomRight = highwayBottomLeft + highwayBottomWidth
    private val highwayTopLeft = (screenWidth - highwayTopWidth) / 2f
    private val highwayTopRight = highwayTopLeft + highwayTopWidth
    // Cap the fret button radius by screen height so landscape doesn't produce
    // oversized circles (width-based lane size is too big when the screen is
    // much wider than tall).
    private val fretButtonRadius = minOf(
        highwayBottomWidth / laneCount / 2f * 0.62f,
        screenHeight * 0.055f,
    )
    private val perspectiveExponent = 2.5f

    // Guitar Power Bar geometry — parallel to highway right edge, bigger
    private val guitarHeight = screenHeight * 0.88f
    private val guitarWidth = guitarHeight * 0.667f  // 2:3 aspect
    private val guitarLeft = highwayBottomRight + 4f  // just right of highway edge
    private val guitarRight = guitarLeft + guitarWidth
    private val guitarTop = (screenHeight - guitarHeight) / 2f
    private val guitarBottom = guitarTop + guitarHeight
    private val guitarCenterX = (guitarLeft + guitarRight) / 2f
    // Fretboard trapezoid — pixel-matched to the guitar PNG inner neck channel
    // Top of neck (y=10%): left=0.501, right=0.568   Bottom of neck (y=55%): left=0.470, right=0.552
    private val fbTopY = guitarTop + guitarHeight * 0.10f
    private val fbBotY = guitarTop + guitarHeight * 0.55f
    private val fbTopLeft = guitarLeft + guitarWidth * 0.501f
    private val fbTopRight = guitarLeft + guitarWidth * 0.568f
    private val fbBotLeft = guitarLeft + guitarWidth * 0.470f
    private val fbBotRight = guitarLeft + guitarWidth * 0.552f
    private val guitarBodyCenterY = guitarTop + guitarHeight * 0.72f
    private val guitarDestRect = RectF(guitarLeft, guitarTop, guitarRight, guitarBottom)
    private val guitarBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guitarGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val guitarGlowHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fretboardFillPath = Path()
    // Smooth fill animation
    private var displayedProgress = 0f
    // Pre-allocated halo smoke particle data: [x, y, size, phase] * 12 particles
    private val haloParticleCount = 12
    private val haloParticleData = FloatArray(haloParticleCount * 4)

    // ── Neon Kineticism Lane Colors ──────────────────────────────────────
    private val laneColors = intArrayOf(
        Color.rgb(80, 225, 249),   // Lane 1 - Cyan (#50E1F9)
        Color.rgb(196, 127, 255),  // Lane 2 - Purple (#C47FFF)
        Color.rgb(255, 231, 146),  // Lane 3 - Gold (#FFE792)
        Color.rgb(74, 222, 128),   // Lane 4 - Green (#4ADE80)
        Color.rgb(255, 107, 107),  // Lane 5 - Coral (#FF6B6B)
    )

    private val laneGlowColors = intArrayOf(
        Color.rgb(128, 238, 255),  // Lane 1 glow
        Color.rgb(217, 168, 255),  // Lane 2 glow
        Color.rgb(255, 240, 179),  // Lane 3 glow
        Color.rgb(134, 239, 172),  // Lane 4 glow
        Color.rgb(255, 153, 153),  // Lane 5 glow
    )

    // --- Video background ---
    private var videoPlayerNormal: VideoBackgroundPlayer? = null
    private var videoPlayerPower: VideoBackgroundPlayer? = null
    private val videoDstRect = RectF(0f, 0f, screenWidth, screenHeight)
    private val videoPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val videoDimPaint = Paint().apply { color = Color.argb(80, 0, 0, 0) } // ~31% dim overlay

    override fun setVideoBackground(normal: VideoBackgroundPlayer?, power: VideoBackgroundPlayer?) {
        videoPlayerNormal = normal
        videoPlayerPower = power
    }

    // --- Pre-allocated Paints ---

    // Background: deep void (#150529)
    private val bgPaint = Paint().apply {
        shader = RadialGradient(
            screenWidth / 2, screenHeight / 2,
            screenHeight * 0.8f,
            intArrayOf(Color.rgb(30, 10, 58), Color.rgb(21, 5, 41)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    // Highway: deep purple tones
    private val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            screenWidth / 2, highwayTopY,
            screenWidth / 2, highwayBottomY,
            Color.rgb(21, 5, 41),
            Color.rgb(42, 16, 80),
            Shader.TileMode.CLAMP
        )
    }

    private val highwayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        shader = LinearGradient(
            0f, 0f, screenWidth, 0f,
            intArrayOf(
                Color.argb(60, 80, 225, 249),
                Color.argb(40, 196, 127, 255),
                Color.argb(60, 80, 225, 249),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.MIRROR
        )
    }

    // Lane dividers: ghost border style (OutlineVariant #514067 at 15%)
    private val laneDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 81, 64, 103) // ~15% of #514067
        strokeWidth = 1.5f
    }

    // Speed lines: primary tinted
    private val speedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, 80, 225, 249)
        strokeWidth = 1.5f
    }

    // Hit line: cyan glow
    private val hitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 225, 249)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val hitLineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 80, 225, 249)
        strokeWidth = 18f
    }

    // Note paints
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(120, 240, 223, 255) // OnBackground muted
    }
    private val noteGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 240, 223, 255)
    }

    // Hold note paints
    private val holdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val holdBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Button paints
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Lane highlight paint
    private val laneHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Hit effect paints
    private val hitCorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hitHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hitSparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 225, 249) // cyan sparks
        strokeWidth = 4f
    }

    // HUD text paints - OnBackground (#f0dfff) instead of pure white
    private val scoreTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(240, 223, 255)
        textSize = 42f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }
    private val scoreLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 240, 223, 255)
        textSize = 15f
        textAlign = Paint.Align.RIGHT
    }
    private val multiplierTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
    }
    private val comboTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val comboGlowTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val streakLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 13f
        color = Color.argb(120, 240, 223, 255)
    }
    private val hitResultTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        textAlign = Paint.Align.CENTER
    }
    private val verticalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 9f
    }
    private val powerMultTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // Power mode red overlay
    private val powerOverlayPaint = Paint().apply {
        color = Color.argb(0, 180, 30, 20)
    }

    // Smoke particle rendering
    private val SMOKE_VARIANT_COUNT = 4
    private val SMOKE_BITMAP_SIZE = 128
    private val smokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val smokeCoreOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
    }
    private val smokeRectF = RectF()
    private var whiteSmokeBitmaps: Array<Bitmap> = emptyArray()
    private var tintedSmokeBitmaps: Array<Array<Bitmap>> = emptyArray() // [laneIndex][textureIndex]
    private var fireSmokeBitmaps: Array<Bitmap> = emptyArray() // red/fire tinted smoke for power mode

    // Progress bar: primary-to-secondary gradient
    private val progressBgPaint = Paint().apply { color = Color.rgb(21, 5, 41) }
    private val progressFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Power bar paints
    private val powerBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarFlamePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Overlay paints
    private val overlayPaint = Paint().apply { color = Color.BLACK }
    private val countdownTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(240, 223, 255)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val countdownGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 225, 249)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pauseTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 64f
        color = Color.rgb(240, 223, 255)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pauseSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.argb(160, 240, 223, 255)
        textSize = 28f
    }

    // Overpress feedback paint
    private val overpressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 82, 82) // Red
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // --- Pre-allocated Paths ---
    private val highwayPath = Path()
    private val laneHighlightPath = Path()
    private val holdTrailPath = Path()
    private val rectF = RectF()
    private val shaderMatrix = Matrix()

    // --- Star field ---
    private val starCount = 50
    private val starData = FloatArray(starCount * 4) // [x, y, baseAlpha, phase] × 50
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(240, 223, 255)
    }

    // --- Side light beams ---
    private val beamPath = Path()
    private val beamPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Pre-cached shaders to avoid per-frame allocation
    private val laneNoteShaders = Array(laneCount) { i ->
        RadialGradient(
            0f, 0f, 1f,
            intArrayOf(Color.argb(240, 240, 223, 255), laneColors[i], Color.rgb(21, 5, 41)),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val fretSocketShaders = Array(laneCount) { i ->
        val cx = laneCenterX(i, 1.0f)
        RadialGradient(cx, highwayBottomY, fretButtonRadius,
            intArrayOf(Color.rgb(42, 16, 80), Color.rgb(21, 5, 41)),
            floatArrayOf(0.8f, 1f), Shader.TileMode.CLAMP)
    }

    private val fretGlowShaders = Array(laneCount) { i ->
        val cx = laneCenterX(i, 1.0f)
        RadialGradient(cx, highwayBottomY, fretButtonRadius * 0.7f,
            intArrayOf(Color.argb(200, 240, 223, 255), laneColors[i], Color.rgb(21, 5, 41)),
            floatArrayOf(0.1f, 0.6f, 1f), Shader.TileMode.CLAMP)
    }

    private val fretPressedGlowShaders = Array(laneCount) { i ->
        val cx = laneCenterX(i, 1.0f)
        RadialGradient(cx, highwayBottomY, fretButtonRadius * 0.9f,
            intArrayOf(Color.argb(200, 240, 223, 255), laneColors[i], Color.rgb(21, 5, 41)),
            floatArrayOf(0.1f, 0.6f, 1f), Shader.TileMode.CLAMP)
    }

    private val progressGradient = LinearGradient(
        0f, 0f, screenWidth, 0f,
        Color.rgb(80, 225, 249),
        Color.rgb(196, 127, 255),
        Shader.TileMode.CLAMP
    )

    init {
        highwayPath.apply {
            moveTo(highwayTopLeft, highwayTopY)
            lineTo(highwayTopRight, highwayTopY)
            lineTo(highwayBottomRight, highwayBottomY)
            lineTo(highwayBottomLeft, highwayBottomY)
            close()
        }

        // Pre-create smoke bitmaps and tinted variants for all 5 lane colors
        whiteSmokeBitmaps = createWhiteSmokeBitmaps()
        tintedSmokeBitmaps = createTintedSmokeBitmaps(whiteSmokeBitmaps, laneColors)
        // Fire-colored smoke for power mode hit effects
        fireSmokeBitmaps = createFireSmokeBitmaps(whiteSmokeBitmaps)

        // Initialize star positions
        val rng = java.util.Random(42)
        for (i in 0 until starCount) {
            starData[i * 4 + 0] = rng.nextFloat() * screenWidth      // x
            starData[i * 4 + 1] = rng.nextFloat() * screenHeight * 0.85f // y (top 85%)
            starData[i * 4 + 2] = 40f + rng.nextFloat() * 80f        // base alpha
            starData[i * 4 + 3] = rng.nextFloat() * 6.28f            // phase offset
        }

        // Initialize halo particle data: phaseOffset, speed, sizeVar, texFrac
        for (i in 0 until haloParticleCount) {
            val off = i * 4
            haloParticleData[off] = rng.nextFloat() * 6.28f       // random phase offset
            haloParticleData[off + 1] = 0.4f + rng.nextFloat() * 0.8f // speed 0.4-1.2
            haloParticleData[off + 2] = rng.nextFloat()           // size variation 0-1
            haloParticleData[off + 3] = rng.nextFloat()           // texture index fraction
        }
    }

    // --- Public getters for GameEngine ---
    override fun getHitLineY(): Float = hitLineY
    override fun getHighwayBottomLeft(): Float = highwayBottomLeft
    override fun getHighwayBottomWidth(): Float = highwayBottomWidth
    override fun getStrikeLineY(): Float = highwayBottomY
    override fun getLaneCenterXAtStrikeline(laneIndex: Int): Float = laneCenterX(laneIndex, 1.0f)
    override fun getLaneColor(laneIndex: Int): Int = laneColors[laneIndex.coerceIn(0, laneCount - 1)]
    override fun getFretPosition(laneIndex: Int): Pair<Float, Float> =
        Pair(laneCenterX(laneIndex, 1.0f), highwayBottomY)
    override fun getParticleDirection(laneIndex: Int): Pair<Float, Float> =
        Pair(0f, -1f)

    // --- Perspective transformation functions ---

    private fun perspectiveY(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return highwayTopY + curved * (highwayBottomY - highwayTopY)
    }

    private fun perspectiveScale(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return 0.08f + 0.92f * curved
    }

    private fun highwayLeftAtProgress(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return highwayTopLeft + curved * (highwayBottomLeft - highwayTopLeft)
    }

    private fun highwayRightAtProgress(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return highwayTopRight + curved * (highwayBottomRight - highwayTopRight)
    }

    private fun laneCenterX(laneIndex: Int, progress: Float): Float {
        val left = highwayLeftAtProgress(progress)
        val right = highwayRightAtProgress(progress)
        val laneWidth = (right - left) / laneCount
        return left + laneWidth * laneIndex + laneWidth / 2f
    }

    private fun noteProgress(activeNote: ActiveNote): Float {
        return (activeNote.y / hitLineY).coerceIn(-0.2f, 1.5f)
    }

    // ========== MAIN RENDER ==========

    override fun render(canvas: Canvas, state: GameState) {
        val frameTimeMs = state.frameTimeMs

        // Layer 1: Background — power video / normal video / gradient fallback
        val activePlayer = if (state.powerActive) videoPlayerPower ?: videoPlayerNormal else videoPlayerNormal
        val vFrame = activePlayer?.currentFrame
        var videoDrawn = false
        if (vFrame != null && !vFrame.isRecycled) {
            try {
                canvas.drawBitmap(vFrame, null, videoDstRect, videoPaint)
                canvas.drawRect(0f, 0f, screenWidth, screenHeight, videoDimPaint)
                videoDrawn = true
            } catch (_: Exception) {
                // Bitmap was recycled between check and draw — fall through to gradient
            }
        }
        if (!videoDrawn) {
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)
        }

        // Red tint overlay when power/special combo is active
        if (state.powerActive) {
            val pulse = (sin(frameTimeMs * 0.004).toFloat() * 0.15f + 0.85f)
            powerOverlayPaint.color = Color.argb((50 * pulse).toInt(), 180, 30, 20)
            canvas.drawRect(0f, 0f, screenWidth, screenHeight, powerOverlayPaint)
        }

        drawStarField(canvas, frameTimeMs)
        drawSideBeams(canvas, frameTimeMs)
        drawHighway(canvas, frameTimeMs)
        drawLaneHighlights(canvas, state)

        for (activeNote in state.activeNotes) {
            if (activeNote.isHit && activeNote.note.isTap) continue
            // Skip trail for completed holds (released or auto-completed)
            if (activeNote.note.isHold && activeNote.isHit) continue
            if (activeNote.note.isHold) drawHoldTrail(canvas, activeNote, frameTimeMs)
        }

        drawStrikeLine(canvas, frameTimeMs)
        drawFretButtons(canvas, state)

        for (activeNote in state.activeNotes) {
            if (activeNote.isHit && activeNote.note.isTap) continue
            // Skip head for active/completed holds (burst already happened on tap)
            if (activeNote.note.isHold && (activeNote.isActive || activeNote.isHit)) continue
            drawNote(canvas, activeNote)
        }

        drawHitEffects(canvas, state, frameTimeMs)
        drawParticles(canvas, state)
        drawPowerBar(canvas, state, frameTimeMs)
        drawHUD(canvas, state)
        drawProgressBar(canvas, state)

        when (state.phase) {
            GamePhase.COUNTDOWN, GamePhase.RESUME_COUNTDOWN -> drawCountdown(canvas, state.countdownValue, frameTimeMs)
            GamePhase.PAUSED -> drawPauseOverlay(canvas)
            else -> {}
        }
    }

    // ========== STAR FIELD ==========

    private fun drawStarField(canvas: Canvas, frameTimeMs: Long) {
        val time = frameTimeMs * 0.001f
        for (i in 0 until starCount) {
            val x = starData[i * 4]
            val y = starData[i * 4 + 1]
            val baseAlpha = starData[i * 4 + 2]
            val phase = starData[i * 4 + 3]
            val alpha = (baseAlpha + sin((time * 1.5f + phase).toDouble()).toFloat() * 40f)
                .toInt().coerceIn(0, 140)
            starPaint.alpha = alpha
            canvas.drawCircle(x, y, 1.5f, starPaint)
        }
    }

    // ========== SIDE BEAMS ==========

    private fun drawSideBeams(canvas: Canvas, frameTimeMs: Long) {
        val pulse = (sin(frameTimeMs * 0.0008).toFloat() + 1f) / 2f
        val beamAlpha = (12 + pulse * 15).toInt()

        // Left beam
        beamPath.reset()
        beamPath.moveTo(0f, 0f)
        beamPath.lineTo(screenWidth * 0.35f, screenHeight)
        beamPath.lineTo(screenWidth * 0.15f, screenHeight)
        beamPath.close()
        beamPaint.shader = LinearGradient(
            0f, 0f, screenWidth * 0.25f, screenHeight,
            Color.argb(beamAlpha, 80, 225, 249),
            Color.argb(0, 80, 225, 249),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(beamPath, beamPaint)

        // Right beam
        beamPath.reset()
        beamPath.moveTo(screenWidth, 0f)
        beamPath.lineTo(screenWidth * 0.65f, screenHeight)
        beamPath.lineTo(screenWidth * 0.85f, screenHeight)
        beamPath.close()
        beamPaint.shader = LinearGradient(
            screenWidth, 0f, screenWidth * 0.75f, screenHeight,
            Color.argb(beamAlpha, 196, 127, 255),
            Color.argb(0, 196, 127, 255),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(beamPath, beamPaint)
        beamPaint.shader = null
    }

    // ========== HIGHWAY ==========

    private fun drawHighway(canvas: Canvas, frameTimeMs: Long) {
        canvas.drawPath(highwayPath, highwayPaint)
        canvas.drawPath(highwayPath, highwayBorderPaint)

        for (i in 1 until laneCount) {
            val topX = highwayTopLeft + (highwayTopRight - highwayTopLeft) * i / laneCount
            val bottomX = highwayBottomLeft + (highwayBottomRight - highwayBottomLeft) * i / laneCount
            canvas.drawLine(topX, highwayTopY, bottomX, highwayBottomY, laneDividerPaint)
        }

        val scrollOffset = (frameTimeMs % 1000) / 1000f
        for (i in 0..10) {
            val baseP = i / 10f
            var p = baseP + scrollOffset * 0.1f
            if (p > 1f) p -= 1f

            val y = perspectiveY(p)
            val left = highwayLeftAtProgress(p)
            val right = highwayRightAtProgress(p)
            val alpha = (sin(p * Math.PI) * 60).toInt().coerceIn(0, 60)
            speedLinePaint.alpha = alpha
            canvas.drawLine(left, y, right, y, speedLinePaint)
        }
    }

    // ========== STRIKELINE ==========

    private fun drawStrikeLine(canvas: Canvas, frameTimeMs: Long) {
        val y = highwayBottomY
        val left = highwayBottomLeft
        val right = highwayBottomRight
        val pulse = (sin(frameTimeMs * 0.01).toFloat() + 1f) / 2f

        hitLineGlowPaint.alpha = (80 + pulse * 120).toInt()
        canvas.drawLine(left, y, right, y, hitLineGlowPaint)

        hitLinePaint.strokeWidth = 4f + pulse * 2f
        canvas.drawLine(left, y, right, y, hitLinePaint)

        // End caps with lane color tint
        buttonPaint.color = Color.rgb(80, 225, 249)
        buttonPaint.shader = null
        canvas.drawCircle(left, y, 8f, buttonPaint)
        canvas.drawCircle(right, y, 8f, buttonPaint)
    }

    // ========== FRET BUTTONS ==========

    private fun drawFretButtons(canvas: Canvas, state: GameState) {
        for (i in 0 until laneCount) {
            val centerX = laneCenterX(i, 1.0f)
            val centerY = highwayBottomY
            val isPressed = (i + 1) in state.pressedLanes
            val color = laneColors[i]
            val radius = fretButtonRadius

            // Base socket - pre-cached shader
            buttonPaint.shader = fretSocketShaders[i]
            canvas.drawCircle(centerX, centerY, radius, buttonPaint)

            // Inner glow - pre-cached shader
            val glowRadius = if (isPressed) radius * 0.9f else radius * 0.7f
            val alpha = if (isPressed) 255 else 100
            buttonPaint.shader = if (isPressed) fretPressedGlowShaders[i] else fretGlowShaders[i]
            buttonPaint.alpha = alpha
            canvas.drawCircle(centerX, centerY, glowRadius, buttonPaint)
            buttonPaint.shader = null
            buttonPaint.alpha = 255

            if (isPressed) {
                buttonGlowPaint.color = color
                buttonGlowPaint.alpha = 150
                canvas.drawCircle(centerX, centerY, radius * 1.4f, buttonGlowPaint)
            }
        }
    }

    // ========== NOTE GEMS ==========

    private fun drawNote(canvas: Canvas, activeNote: ActiveNote) {
        val note = activeNote.note
        val laneIndex = note.lane - 1
        if (laneIndex !in 0 until laneCount) return

        val progress = noteProgress(activeNote)
        if (progress < -0.05f || progress > 1.3f) return

        val screenY = perspectiveY(progress)
        val scale = perspectiveScale(progress)
        val centerX = laneCenterX(laneIndex, progress)
        val color = laneColors[laneIndex]
        val glowColor = laneGlowColors[laneIndex]

        val baseRadius = fretButtonRadius * 0.85f
        val radius = baseRadius * scale

        if (radius < 2f) return

        // Proximity glow - wider ambient glow
        val distToStrike = abs(progress - 1f)
        if (distToStrike < 0.4f) {
            val intensity = 1f - distToStrike / 0.4f
            noteGlowPaint.color = glowColor
            noteGlowPaint.alpha = (intensity * 200).toInt()
            canvas.drawCircle(centerX, screenY, radius * 2.0f, noteGlowPaint)
        }

        // Gem body - use pre-cached shader with matrix translation
        val noteShader = laneNoteShaders[laneIndex]
        shaderMatrix.reset()
        shaderMatrix.setScale(radius * 1.2f, radius * 1.2f)
        shaderMatrix.postTranslate(centerX - radius * 0.3f, screenY - radius * 0.3f)
        noteShader.setLocalMatrix(shaderMatrix)
        notePaint.shader = noteShader
        canvas.drawCircle(centerX, screenY, radius, notePaint)
        notePaint.shader = null

        // Outer ring
        noteRimPaint.strokeWidth = 2f * scale
        noteRimPaint.color = Color.argb(60, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawCircle(centerX, screenY, radius * 1.25f, noteRimPaint)

        // Inner rim (restore color)
        noteRimPaint.color = Color.argb(120, 240, 223, 255)
        noteRimPaint.strokeWidth = 3f * scale
        canvas.drawCircle(centerX, screenY, radius, noteRimPaint)

        // Center dot
        canvas.drawCircle(centerX, screenY, radius * 0.3f, noteCenterPaint)
    }

    // ========== HOLD TRAILS ==========

    private fun drawHoldTrail(canvas: Canvas, activeNote: ActiveNote, frameTimeMs: Long) {
        val note = activeNote.note
        val laneIndex = note.lane - 1
        if (laneIndex !in 0 until laneCount) return
        val color = laneColors[laneIndex]

        val holdProgressSpan = note.durationMs.toFloat() / approachTimeMsF

        // While held: head clamped at hit line, tail shrinks as hold progresses
        val headProgress: Float
        val tailProgress: Float
        if (activeNote.isActive) {
            headProgress = 1.0f
            val remainingFraction = (1f - activeNote.holdProgress).coerceIn(0f, 1f)
            tailProgress = 1.0f - holdProgressSpan * remainingFraction
        } else {
            headProgress = noteProgress(activeNote)
            tailProgress = headProgress - holdProgressSpan
        }

        val trailHalfWidth = fretButtonRadius * 0.38f
        val steps = 20

        holdTrailPath.reset()

        // Left edge
        for (i in 0..steps) {
            val t = tailProgress + (headProgress - tailProgress) * i / steps
            val tc = t.coerceIn(0f, 1.2f)
            val y = perspectiveY(tc)
            val cx = laneCenterX(laneIndex, tc)
            val s = perspectiveScale(tc)
            val halfW = trailHalfWidth * s
            if (i == 0) holdTrailPath.moveTo(cx - halfW, y)
            else holdTrailPath.lineTo(cx - halfW, y)
        }
        // Right edge (reverse)
        for (i in steps downTo 0) {
            val t = tailProgress + (headProgress - tailProgress) * i / steps
            val tc = t.coerceIn(0f, 1.2f)
            val y = perspectiveY(tc)
            val cx = laneCenterX(laneIndex, tc)
            val s = perspectiveScale(tc)
            val halfW = trailHalfWidth * s
            holdTrailPath.lineTo(cx + halfW, y)
        }
        holdTrailPath.close()

        // Alpha gradient: transparent at vanishing point, opaque near head
        val topY = perspectiveY(tailProgress.coerceAtLeast(0f))
        val bottomY = perspectiveY(headProgress.coerceAtMost(1.2f))
        holdPaint.shader = LinearGradient(
            0f, topY, 0f, bottomY,
            Color.argb(0, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(140, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        )
        holdPaint.style = Paint.Style.FILL
        canvas.drawPath(holdTrailPath, holdPaint)
        holdPaint.shader = null

        // Pulsing border
        val pulse = ((sin(frameTimeMs * 0.006).toFloat() + 1f) / 2f * 70 + 50).toInt()
        holdBorderPaint.color = Color.argb(
            pulse, Color.red(color), Color.green(color), Color.blue(color)
        )
        canvas.drawPath(holdTrailPath, holdBorderPaint)

        // Center light strip — thin dots from tail to head
        for (i in 0..steps) {
            val t = tailProgress + (headProgress - tailProgress) * i / steps
            val tc = t.coerceIn(0f, 1.2f)
            val y = perspectiveY(tc)
            val cx = laneCenterX(laneIndex, tc)
            val fadeProgress = i.toFloat() / steps  // 0 = tail, 1 = head
            val lineAlpha = (fadeProgress * 120).toInt()
            noteCenterPaint.alpha = lineAlpha
            canvas.drawCircle(cx, y, 1.5f * perspectiveScale(tc), noteCenterPaint)
        }
        noteCenterPaint.alpha = 200
    }

    // ========== LANE HIGHLIGHTS ==========

    private fun drawLaneHighlights(canvas: Canvas, state: GameState) {
        for (lane in state.pressedLanes) {
            val laneIndex = lane - 1
            if (laneIndex !in 0 until laneCount) continue
            val color = laneColors[laneIndex]

            laneHighlightPath.reset()
            val steps = 16

            for (i in 0..steps) {
                val p = i.toFloat() / steps
                val y = perspectiveY(p)
                val left = highwayLeftAtProgress(p)
                val right = highwayRightAtProgress(p)
                val lw = (right - left) / laneCount
                val x = left + lw * laneIndex
                if (i == 0) laneHighlightPath.moveTo(x, y)
                else laneHighlightPath.lineTo(x, y)
            }
            for (i in steps downTo 0) {
                val p = i.toFloat() / steps
                val y = perspectiveY(p)
                val left = highwayLeftAtProgress(p)
                val right = highwayRightAtProgress(p)
                val lw = (right - left) / laneCount
                val x = left + lw * (laneIndex + 1)
                laneHighlightPath.lineTo(x, y)
            }
            laneHighlightPath.close()

            laneHighlightPaint.shader = null
            laneHighlightPaint.color = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawPath(laneHighlightPath, laneHighlightPaint)
        }
    }

    // ========== SMOKE PARTICLES ==========

    private fun drawParticles(canvas: Canvas, state: GameState) {
        if (!state.particlesEnabled) return
        for (p in state.particles) {
            val alpha = (p.life * 255).toInt().coerceIn(0, 255)
            if (alpha <= 0) continue

            val laneIdx = p.laneIndex.coerceIn(0, laneCount - 1)
            val texIdx = p.textureIndex.coerceIn(0, SMOKE_VARIANT_COUNT - 1)

            val halfSize = p.size
            smokeRectF.set(p.x - halfSize, p.y - halfSize, p.x + halfSize, p.y + halfSize)

            // Tinted smoke layer with additive blending (neon glow)
            smokePaint.alpha = alpha
            canvas.drawBitmap(tintedSmokeBitmaps[laneIdx][texIdx], null, smokeRectF, smokePaint)

            // White core overlay with additive blending (bright center)
            smokeCoreOverlayPaint.alpha = (alpha * 0.4f).toInt().coerceIn(0, 255)
            canvas.drawBitmap(whiteSmokeBitmaps[texIdx], null, smokeRectF, smokeCoreOverlayPaint)
        }
    }

    // ========== SMOKE BITMAP GENERATION ==========

    private fun createWhiteSmokeBitmaps(): Array<Bitmap> {
        val size = SMOKE_BITMAP_SIZE
        val center = size / 2f

        return Array(SMOKE_VARIANT_COUNT) { variant ->
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            val radius = when (variant) {
                0 -> size * 0.35f   // tight core
                1 -> size * 0.40f   // medium
                2 -> size * 0.45f   // diffuse
                3 -> size * 0.48f   // very diffuse
                else -> size * 0.40f
            }

            val innerAlpha = when (variant) {
                0 -> 255; 1 -> 230; 2 -> 200; 3 -> 180; else -> 220
            }
            val midAlpha = when (variant) {
                0 -> 200; 1 -> 160; 2 -> 130; 3 -> 100; else -> 150
            }

            paint.shader = RadialGradient(
                center, center, radius,
                intArrayOf(
                    Color.argb(innerAlpha, 255, 255, 255),
                    Color.argb(midAlpha, 255, 255, 255),
                    Color.argb(0, 255, 255, 255),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            c.drawCircle(center, center, radius, paint)
            bitmap
        }
    }

    private fun createTintedSmokeBitmaps(bases: Array<Bitmap>, colors: IntArray): Array<Array<Bitmap>> {
        val tintPaint = Paint()

        return Array(colors.size) { colorIdx ->
            Array(bases.size) { texIdx ->
                val base = bases[texIdx]
                val tinted = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
                val c = Canvas(tinted)

                // Draw the white smoke shape
                tintPaint.xfermode = null
                c.drawBitmap(base, 0f, 0f, tintPaint)

                // Apply lane color via SRC_IN — fills the shape with the color
                tintPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                tintPaint.color = colors[colorIdx]
                c.drawRect(0f, 0f, base.width.toFloat(), base.height.toFloat(), tintPaint)
                tintPaint.xfermode = null

                tinted
            }
        }
    }

    // ========== FIRE SMOKE BITMAPS (power mode) ==========

    private fun createFireSmokeBitmaps(bases: Array<Bitmap>): Array<Bitmap> {
        val tintPaint = Paint()
        val fireColor = Color.rgb(220, 60, 20)
        return Array(bases.size) { texIdx ->
            val base = bases[texIdx]
            val tinted = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val c = Canvas(tinted)
            tintPaint.xfermode = null
            c.drawBitmap(base, 0f, 0f, tintPaint)
            tintPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            tintPaint.color = fireColor
            c.drawRect(0f, 0f, base.width.toFloat(), base.height.toFloat(), tintPaint)
            tintPaint.xfermode = null
            tinted
        }
    }

    // ========== HIT EFFECTS ==========

    private fun drawHitEffects(canvas: Canvas, state: GameState, frameTimeMs: Long) {
        val isPowerMode = state.powerActive
        for (effect in state.hitEffects) {
            val age = frameTimeMs - effect.createdAt
            val maxAge = if (isPowerMode) 600L else 400L
            if (age > maxAge) continue

            val progress = age / maxAge.toFloat()
            val laneIndex = effect.lane - 1
            if (laneIndex !in 0 until laneCount) continue

            val centerX = laneCenterX(laneIndex, 1.0f)
            val centerY = highwayBottomY

            // Overpress / Miss: red pulse + X effect
            if (effect.result == HitResult.MISS) {
                val missAlpha = ((1f - progress) * 255).toInt()

                // Red pulse ring expanding outward
                hitHaloPaint.color = Color.rgb(255, 82, 82)
                hitHaloPaint.alpha = (missAlpha * 0.5f).toInt()
                hitHaloPaint.style = Paint.Style.STROKE
                hitHaloPaint.strokeWidth = 3f
                canvas.drawCircle(centerX, centerY, fretButtonRadius * (0.8f + progress * 1.5f), hitHaloPaint)
                hitHaloPaint.style = Paint.Style.FILL

                // Red flash fill on the fret
                hitCorePaint.color = Color.rgb(255, 50, 50)
                hitCorePaint.alpha = (missAlpha * 0.3f).toInt()
                hitCorePaint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY, fretButtonRadius * (1f - progress * 0.3f), hitCorePaint)

                // X text floating up
                overpressPaint.alpha = missAlpha
                val textY = centerY - 60f - progress * 40f
                canvas.drawText("X", centerX, textY, overpressPaint)
                continue
            }

            if (isPowerMode) {
                // Fire smoke effect during power mode
                val smokeAlpha = ((1f - progress) * 220).toInt().coerceIn(0, 255)
                val smokeSize = fretButtonRadius * (1.5f + progress * 3f)
                for (i in 0..2) {
                    val texIdx = (effect.lane + i) % SMOKE_VARIANT_COUNT
                    val offsetX = effect.sparkOffsetsX[i] * smokeSize * 0.6f
                    val offsetY = progress * smokeSize * 1.2f + effect.sparkOffsetsY[i] * smokeSize * 0.4f
                    val halfS = smokeSize * (0.6f + i * 0.15f)
                    smokeRectF.set(
                        centerX + offsetX - halfS,
                        centerY - offsetY - halfS,
                        centerX + offsetX + halfS,
                        centerY - offsetY + halfS,
                    )
                    smokePaint.alpha = (smokeAlpha * (1f - i * 0.25f)).toInt().coerceIn(0, 255)
                    canvas.drawBitmap(fireSmokeBitmaps[texIdx], null, smokeRectF, smokePaint)
                }

                // Fire core burst
                hitCorePaint.color = Color.rgb(255, 100, 30)
                hitCorePaint.alpha = smokeAlpha
                hitCorePaint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY, fretButtonRadius * (0.5f + progress * 1.2f), hitCorePaint)

                // Fire halo
                hitHaloPaint.color = Color.rgb(200, 50, 20)
                hitHaloPaint.alpha = (smokeAlpha * 0.5f).toInt()
                canvas.drawCircle(centerX, centerY, fretButtonRadius * (0.8f + progress * 1.8f), hitHaloPaint)
            } else {
                val burstSize = fretButtonRadius * (1f + progress * 2f)
                val alpha = ((1f - progress) * 255).toInt()

                // Core burst
                hitCorePaint.color = Color.rgb(240, 223, 255)
                hitCorePaint.alpha = alpha
                hitCorePaint.style = Paint.Style.FILL
                canvas.drawCircle(centerX, centerY, burstSize * 0.4f, hitCorePaint)

                // Halo
                val haloColor = if (effect.result == HitResult.PERFECT) {
                    Color.rgb(80, 225, 249)
                } else {
                    Color.rgb(196, 127, 255)
                }
                hitHaloPaint.color = haloColor
                hitHaloPaint.alpha = (alpha * 0.7f).toInt()
                canvas.drawCircle(centerX, centerY, burstSize * 0.8f, hitHaloPaint)

                // Sparks
                for (i in 0..4) {
                    val sparkOffsetX = effect.sparkOffsetsX[i] * burstSize
                    val sparkOffsetY = progress * 150f + effect.sparkOffsetsY[i] * 50f
                    canvas.drawPoint(centerX + sparkOffsetX, centerY - sparkOffsetY, hitSparkPaint)
                }
            }

            // Result text
            val textY = centerY - 100f - progress * 50f
            hitResultTextPaint.color = if (isPowerMode) Color.rgb(255, 200, 100) else Color.rgb(240, 223, 255)
            val text = when (effect.result) {
                HitResult.PERFECT -> "PERFECT!"
                HitResult.GREAT -> "GREAT"
                HitResult.GOOD -> "GOOD"
                HitResult.MISS -> "MISS"
            }
            canvas.drawText(text, centerX, textY, hitResultTextPaint)
        }
    }

    // ========== GUITAR POWER BAR ==========

    /** Interpolate fretboard left/right edges at a given Y fraction (0=top, 1=bottom of neck) */
    private fun fbEdgeLeft(t: Float) = fbTopLeft + (fbBotLeft - fbTopLeft) * t
    private fun fbEdgeRight(t: Float) = fbTopRight + (fbBotRight - fbTopRight) * t

    private fun drawPowerBar(canvas: Canvas, state: GameState, frameTimeMs: Long) {
        if (guitarBitmap == null) return
        val targetProgress = state.powerBarProgress.coerceIn(0f, 1f)

        // Smooth lerp toward target (fast catch-up, smooth visual)
        displayedProgress += (targetProgress - displayedProgress) * 0.08f
        if (abs(displayedProgress - targetProgress) < 0.002f) displayedProgress = targetProgress
        val progress = displayedProgress.coerceIn(0f, 1f)
        val isFull = progress >= 0.99f

        // 1. Always draw empty guitar as base
        guitarBitmapPaint.alpha = 255
        canvas.drawBitmap(guitarBitmap, null, guitarDestRect, guitarBitmapPaint)

        // 2. Clip full guitar from bottom up based on smooth progress
        if (progress > 0.001f && guitarFullBitmap != null) {
            val clipTop = guitarBottom - guitarHeight * progress
            canvas.save()
            canvas.clipRect(guitarLeft, clipTop, guitarRight, guitarBottom)
            guitarBitmapPaint.alpha = 255
            canvas.drawBitmap(guitarFullBitmap, null, guitarDestRect, guitarBitmapPaint)
            canvas.restore()
        }

        // 3. Fire smoke halo when full — same bitmap particles as hit effects
        if (isFull && !state.powerActive && fireSmokeBitmaps.isNotEmpty() && whiteSmokeBitmaps.isNotEmpty()) {
            val t = frameTimeMs * 0.003f
            val baseSize = guitarWidth * 0.45f

            // Rising fire smoke wisps around guitar body
            for (i in 0 until haloParticleCount) {
                val off = i * 4
                val phase = t + haloParticleData[off]  // pre-stored phase offset
                val speed = haloParticleData[off + 1]   // speed multiplier
                val sizeVar = haloParticleData[off + 2]  // size variation
                val texOff = haloParticleData[off + 3]   // texture index fraction

                // Swirl around guitar center, rise upward
                val angle = phase + t * speed
                val radius = guitarWidth * (0.25f + sin((phase * 0.4f).toDouble()).toFloat() * 0.15f)
                val px = guitarCenterX + sin(angle.toDouble()).toFloat() * radius
                val riseCycle = ((frameTimeMs + (phase * 500f).toLong()) % 3000L) / 3000f
                val py = guitarTop + guitarHeight * (0.85f - riseCycle * 0.9f)

                val alpha = ((1f - riseCycle) * 140).toInt().coerceIn(0, 255)
                if (alpha <= 0) continue

                val halfS = baseSize * (0.3f + sizeVar * 0.4f + riseCycle * 0.3f)
                val texIdx = (texOff * SMOKE_VARIANT_COUNT).toInt().coerceIn(0, SMOKE_VARIANT_COUNT - 1)

                smokeRectF.set(px - halfS, py - halfS, px + halfS, py + halfS)

                // Fire smoke layer (additive blend)
                smokePaint.alpha = alpha
                canvas.drawBitmap(fireSmokeBitmaps[texIdx], null, smokeRectF, smokePaint)

                // White core overlay for bright center
                smokeCoreOverlayPaint.alpha = (alpha * 0.35f).toInt().coerceIn(0, 255)
                canvas.drawBitmap(whiteSmokeBitmaps[texIdx], null, smokeRectF, smokeCoreOverlayPaint)
            }
        }

        // 4. Active power: show multiplier on body
        if (state.powerActive) {
            val pulse = (sin(frameTimeMs * 0.006).toFloat() + 1f) / 2f
            val multAlpha = (180 + (pulse * 75).toInt()).coerceIn(0, 255)
            powerMultTextPaint.textSize = guitarWidth * 0.24f
            powerMultTextPaint.color = Color.argb(multAlpha, 255, 231, 146)
            canvas.drawText("${state.powerMultiplier.toInt()}X", guitarCenterX, guitarBodyCenterY, powerMultTextPaint)
        }
    }

    // ========== HUD ==========

    private fun drawHUD(canvas: Canvas, state: GameState) {
        val padding = 20f

        val scoreX = screenWidth - padding
        val scoreY = screenHeight * 0.05f + 40f
        canvas.drawText("${state.score}", scoreX, scoreY, scoreTextPaint)
        canvas.drawText("SCORE", scoreX, scoreY + 17f, scoreLabelPaint)

        // Multiplier with Neon Kineticism colors
        if (state.comboMultiplier > 1.0f) {
            multiplierTextPaint.color = when {
                state.comboMultiplier >= 4f -> Color.rgb(255, 231, 146) // gold
                state.comboMultiplier >= 3f -> Color.rgb(196, 127, 255) // purple
                state.comboMultiplier >= 2f -> Color.rgb(80, 225, 249)  // cyan
                else -> Color.rgb(240, 223, 255)
            }
            canvas.drawText("${state.comboMultiplier.toInt()}x", scoreX, scoreY + 50f, multiplierTextPaint)
        }

        // Combo - milestone colors: cyan 20+, purple 50+, gold 100+
        if (state.combo > 2) {
            val comboX = screenWidth / 2
            val comboY = highwayBottomY - fretButtonRadius * 2.8f

            val comboSize = when {
                state.combo >= 100 -> 46f
                state.combo >= 50 -> 40f
                state.combo >= 20 -> 34f
                else -> 28f
            }

            if (state.combo >= 20) {
                val glowColor = when {
                    state.combo >= 100 -> Color.rgb(255, 231, 146) // gold
                    state.combo >= 50 -> Color.rgb(196, 127, 255)  // purple
                    else -> Color.rgb(80, 225, 249)                 // cyan
                }
                comboGlowTextPaint.color = glowColor
                comboGlowTextPaint.alpha = 90
                comboGlowTextPaint.textSize = comboSize + 4f
                canvas.drawText("${state.combo}", comboX, comboY, comboGlowTextPaint)
            }

            val comboColor = when {
                state.combo >= 100 -> Color.rgb(255, 231, 146) // gold
                state.combo >= 50 -> Color.rgb(196, 127, 255)  // purple
                state.combo >= 20 -> Color.rgb(80, 225, 249)   // cyan
                else -> Color.rgb(240, 223, 255)                // text primary
            }
            comboTextPaint.color = comboColor
            comboTextPaint.textSize = comboSize
            canvas.drawText("${state.combo}", comboX, comboY, comboTextPaint)

            canvas.drawText("NOTE STREAK", comboX, comboY + 16f, streakLabelPaint)
        }
    }

    // ========== PROGRESS BAR ==========

    private fun drawProgressBar(canvas: Canvas, state: GameState) {
        val barHeight = 5f
        canvas.drawRect(0f, 0f, screenWidth, barHeight, progressBgPaint)

        if (state.songDurationMs > 0) {
            val progress = (state.currentTimeMs.toFloat() / state.songDurationMs).coerceIn(0f, 1f)
            val fillWidth = screenWidth * progress

            // Primary-to-secondary gradient
            progressFillPaint.shader = progressGradient
            canvas.drawRect(0f, 0f, fillWidth, barHeight, progressFillPaint)
            progressFillPaint.shader = null
        }
    }

    // ========== COUNTDOWN ==========

    private fun drawCountdown(canvas: Canvas, value: Int, frameTimeMs: Long) {
        overlayPaint.alpha = 170
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        val scale = 1f + (sin(frameTimeMs * 0.01).toFloat() + 1f) * 0.05f

        countdownTextPaint.textSize = 180f * scale
        canvas.drawText(value.toString(), screenWidth / 2, screenHeight / 2 + 60f, countdownTextPaint)

        countdownGlowPaint.textSize = 180f * scale
        countdownGlowPaint.alpha = 120
        canvas.drawText(value.toString(), screenWidth / 2, screenHeight / 2 + 60f, countdownGlowPaint)
    }

    // ========== PAUSE OVERLAY ==========

    private fun drawPauseOverlay(canvas: Canvas) {
        overlayPaint.alpha = 190
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        canvas.drawText("PAUSED", screenWidth / 2, screenHeight / 2 - 20f, pauseTitlePaint)
        canvas.drawText("Tap to resume", screenWidth / 2, screenHeight / 2 + 30f, pauseSubtitlePaint)
    }
}
