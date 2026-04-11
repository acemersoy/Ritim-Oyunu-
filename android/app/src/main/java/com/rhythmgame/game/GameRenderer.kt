package com.rhythmgame.game

import android.graphics.*
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(
    private val screenWidth: Float,
    private val screenHeight: Float,
) : IGameRenderer {
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

    // Power Bar geometry
    private val powerBarRight = screenWidth - 10f
    private val powerBarBarWidth = 20f
    private val powerBarLeft = powerBarRight - powerBarBarWidth
    private val powerBarTop = screenHeight * 0.25f
    private val powerBarBottom = screenHeight * 0.72f
    private val powerBarHeight = powerBarBottom - powerBarTop

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

        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)
        drawHighway(canvas, frameTimeMs)
        drawLaneHighlights(canvas, state)

        for (activeNote in state.activeNotes) {
            if (activeNote.isHit && activeNote.note.isTap) continue
            if (activeNote.note.isHold) drawHoldTrail(canvas, activeNote, frameTimeMs)
        }

        drawStrikeLine(canvas, frameTimeMs)
        drawFretButtons(canvas, state)

        for (activeNote in state.activeNotes) {
            if (activeNote.isHit && activeNote.note.isTap) continue
            drawNote(canvas, activeNote)
        }

        drawHitEffects(canvas, state, frameTimeMs)
        drawParticles(canvas, state)
        drawPowerBar(canvas, state, frameTimeMs)
        drawHUD(canvas, state)
        drawProgressBar(canvas, state)

        when (state.phase) {
            GamePhase.COUNTDOWN -> drawCountdown(canvas, state.countdownValue, frameTimeMs)
            GamePhase.PAUSED -> drawPauseOverlay(canvas)
            else -> {}
        }
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

        // Proximity glow - more intense, wider ambient glow
        val distToStrike = abs(progress - 1f)
        if (distToStrike < 0.35f) {
            val intensity = 1f - distToStrike / 0.35f
            noteGlowPaint.color = glowColor
            noteGlowPaint.alpha = (intensity * 220).toInt()
            canvas.drawCircle(centerX, screenY, radius * 1.7f, noteGlowPaint)
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

        // Rim
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

        val headProgress = noteProgress(activeNote)
        val holdProgressSpan = note.durationMs.toFloat() / 2000f
        val tailProgress = (headProgress - holdProgressSpan).coerceAtLeast(-0.1f)

        val trailHalfWidth = fretButtonRadius * 0.32f
        val steps = 16

        holdTrailPath.reset()

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

        holdPaint.shader = null
        holdPaint.style = Paint.Style.FILL
        holdPaint.color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawPath(holdTrailPath, holdPaint)

        val pulse = ((sin(frameTimeMs * 0.006).toFloat() + 1f) / 2f * 70 + 50).toInt()
        holdBorderPaint.color = Color.argb(
            pulse, Color.red(color), Color.green(color), Color.blue(color)
        )
        canvas.drawPath(holdTrailPath, holdBorderPaint)
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

    // ========== HIT EFFECTS ==========

    private fun drawHitEffects(canvas: Canvas, state: GameState, frameTimeMs: Long) {
        for (effect in state.hitEffects) {
            val age = frameTimeMs - effect.createdAt
            if (age > 400) continue

            val progress = age / 400f
            val laneIndex = effect.lane - 1
            if (laneIndex !in 0 until laneCount) continue

            val centerX = laneCenterX(laneIndex, 1.0f)
            val centerY = highwayBottomY

            // Overpress: red X effect
            if (effect.result == HitResult.MISS) {
                val missAlpha = ((1f - progress) * 255).toInt()
                overpressPaint.alpha = missAlpha
                val textY = centerY - 60f - progress * 40f
                canvas.drawText("X", centerX, textY, overpressPaint)
                continue
            }

            val burstSize = fretButtonRadius * (1f + progress * 2f)
            val alpha = ((1f - progress) * 255).toInt()

            // Core burst - cyan tinted
            hitCorePaint.color = Color.rgb(240, 223, 255)
            hitCorePaint.alpha = alpha
            hitCorePaint.style = Paint.Style.FILL
            canvas.drawCircle(centerX, centerY, burstSize * 0.4f, hitCorePaint)

            // Halo - primary/secondary tinted
            val haloColor = if (effect.result == HitResult.PERFECT) {
                Color.rgb(80, 225, 249) // cyan
            } else {
                Color.rgb(196, 127, 255) // purple
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

            // Result text
            val textY = centerY - 100f - progress * 50f
            hitResultTextPaint.color = Color.rgb(240, 223, 255)
            val text = when (effect.result) {
                HitResult.PERFECT -> "PERFECT!"
                HitResult.GREAT -> "GREAT"
                HitResult.GOOD -> "GOOD"
                HitResult.MISS -> "MISS"
            }
            canvas.drawText(text, centerX, textY, hitResultTextPaint)
        }
    }

    // ========== POWER BAR ==========

    private fun drawPowerBar(canvas: Canvas, state: GameState, frameTimeMs: Long) {
        val barLeft = powerBarLeft
        val barRight = powerBarRight
        val barTop = powerBarTop
        val barBottom = powerBarBottom
        val barHeight = powerBarHeight
        val barCenterX = (barLeft + barRight) / 2f
        val progress = state.powerBarProgress.coerceIn(0f, 1f)

        powerBarBgPaint.style = Paint.Style.FILL
        powerBarBgPaint.shader = null
        powerBarBgPaint.color = Color.argb(60, 42, 16, 80)
        rectF.set(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rectF, 6f, 6f, powerBarBgPaint)

        val tierLineY = barBottom - barHeight * 0.5f
        powerBarBgPaint.color = Color.argb(60, 255, 231, 146) // gold tier line
        canvas.drawLine(barLeft + 2f, tierLineY, barRight - 2f, tierLineY, powerBarBgPaint)

        if (progress > 0.001f) {
            val fillHeight = barHeight * progress
            val fillTop = barBottom - fillHeight

            // Tertiary (#FFE792) highlight for power bar
            val fillColor = when {
                progress >= 0.7f -> Color.rgb(255, 231, 146) // gold
                progress >= 0.3f -> Color.rgb(196, 127, 255) // purple
                else -> Color.rgb(80, 225, 249)              // cyan
            }
            powerBarFillPaint.shader = null
            powerBarFillPaint.style = Paint.Style.FILL
            powerBarFillPaint.color = fillColor

            canvas.save()
            canvas.clipRect(barLeft, fillTop, barRight, barBottom)
            rectF.set(barLeft, barTop, barRight, barBottom)
            canvas.drawRoundRect(rectF, 6f, 6f, powerBarFillPaint)
            canvas.restore()

            val flameCount = if (progress > 0.5f) 5 else 3
            for (i in 0 until flameCount) {
                val phase = frameTimeMs * 0.004f + i * 1.3f
                val flameX = barCenterX + sin(phase.toDouble()).toFloat() * powerBarBarWidth * 0.4f
                val flameY = fillTop - 3f - abs(sin((phase * 1.5f).toDouble())).toFloat() * 14f
                val flameSize = 2.5f + sin((phase * 0.7f).toDouble()).toFloat() * 1.5f
                val flameAlpha = (130 + (sin(phase.toDouble()) * 80).toInt()).coerceIn(0, 255)
                powerBarFlamePaint.color = Color.argb(flameAlpha, 255, 231, 146)
                canvas.drawCircle(flameX, flameY, flameSize, powerBarFlamePaint)
            }
        }

        val borderColor = when {
            state.powerActive -> {
                val pulse = (sin(frameTimeMs * 0.008).toFloat() + 1f) / 2f
                Color.argb((150 + (pulse * 105).toInt()).coerceIn(0, 255), 255, 231, 146)
            }
            progress >= 1f -> {
                val pulse = (sin(frameTimeMs * 0.005).toFloat() + 1f) / 2f
                Color.argb((100 + (pulse * 100).toInt()).coerceIn(0, 255), 196, 127, 255)
            }
            progress >= 0.5f -> Color.argb(100, 196, 127, 255)
            else -> Color.argb(40, 80, 225, 249)
        }
        powerBarBgPaint.style = Paint.Style.STROKE
        powerBarBgPaint.strokeWidth = if (state.powerActive) 3f else 1.5f
        powerBarBgPaint.color = borderColor
        rectF.set(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rectF, 6f, 6f, powerBarBgPaint)
        powerBarBgPaint.style = Paint.Style.FILL

        if (state.powerActive) {
            val pulse = (sin(frameTimeMs * 0.006).toFloat() + 1f) / 2f
            powerBarFlamePaint.color = Color.argb(
                (40 + (pulse * 60).toInt()).coerceIn(0, 255), 255, 231, 146,
            )
            rectF.set(barLeft - 4f, barTop - 4f, barRight + 4f, barBottom + 4f)
            canvas.drawRoundRect(rectF, 8f, 8f, powerBarFlamePaint)
        }

        if (state.powerActive) {
            powerMultTextPaint.textSize = 18f
            powerMultTextPaint.color = Color.rgb(255, 231, 146)
            canvas.drawText("${state.powerMultiplier.toInt()}X", barCenterX, barTop - 10f, powerMultTextPaint)
        } else if (progress >= 0.5f) {
            val multText = if (progress >= 1f) "4X" else "2X"
            val pulse = (sin(frameTimeMs * 0.004).toFloat() + 1f) / 2f
            val alpha = (120 + (pulse * 135).toInt()).coerceIn(0, 255)
            powerMultTextPaint.textSize = 14f
            powerMultTextPaint.color = Color.argb(alpha, 255, 231, 146)
            canvas.drawText(multText, barCenterX, barTop - 6f, powerMultTextPaint)
        }

        verticalLabelPaint.color = Color.argb(80, 255, 231, 146)
        val labelChars = "POWER"
        for (i in labelChars.indices) {
            canvas.drawText(
                labelChars[i].toString(),
                barCenterX,
                barBottom + 13f + i * 10f,
                verticalLabelPaint,
            )
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
