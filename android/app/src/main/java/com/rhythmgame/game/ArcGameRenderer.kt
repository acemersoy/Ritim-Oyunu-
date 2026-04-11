package com.rhythmgame.game

import android.graphics.*
import kotlin.math.*

class ArcGameRenderer(
    private val screenWidth: Float,
    private val screenHeight: Float,
) : IGameRenderer {

    private val laneCount = 5

    // ── Arc Geometry ──────────────────────────────────────────────────────────
    // Center (apex) of the arc fan — top center of screen
    private val centerX = screenWidth / 2f
    private val centerY = screenHeight * 0.06f

    // Radii — notes travel from innerRadius to outerRadius
    private val innerRadius = screenHeight * 0.12f
    private val outerRadius = screenHeight * 0.88f
    private val hitLineRadius = outerRadius

    // Angular span of the fan: -65° to +65° (130° total), measured from 12-o'clock (downward)
    private val halfSpanDeg = 65f
    private val halfSpanRad = Math.toRadians(halfSpanDeg.toDouble()).toFloat()
    private val totalSpanRad = halfSpanRad * 2f

    // Perspective exponent (same steepness as highway renderer)
    private val perspectiveExponent = 2.5f

    // Fret button radius — same formula as highway, adapted for arc
    // Use the arc chord length at outer radius as "highway bottom width"
    private val arcBottomChordLength = 2f * outerRadius * sin(halfSpanRad)
    private val effectiveHighwayWidth = arcBottomChordLength * 0.62f
    private val fretButtonRadius = minOf(
        effectiveHighwayWidth / laneCount / 2f * 0.62f,
        screenHeight * 0.055f,
    )

    // For IGameRenderer compatibility: approximate the hit line area with a bounding box
    private val hitLineY = centerY + outerRadius * cos(0f) // straight down from center
    private val highwayBottomWidth = effectiveHighwayWidth
    private val highwayBottomLeft = (screenWidth - highwayBottomWidth) / 2f

    // Power Bar geometry (same as highway renderer)
    private val powerBarRight = screenWidth - 10f
    private val powerBarBarWidth = 20f
    private val powerBarLeft = powerBarRight - powerBarBarWidth
    private val powerBarTop = screenHeight * 0.25f
    private val powerBarBottom = screenHeight * 0.72f
    private val powerBarHeight = powerBarBottom - powerBarTop

    // ── Lane Colors ──────────────────────────────────────────────────────────
    private val laneColors = intArrayOf(
        Color.rgb(80, 225, 249),   // Cyan
        Color.rgb(196, 127, 255),  // Purple
        Color.rgb(255, 231, 146),  // Gold
        Color.rgb(74, 222, 128),   // Green
        Color.rgb(255, 107, 107),  // Coral
    )

    private val laneGlowColors = intArrayOf(
        Color.rgb(128, 238, 255),
        Color.rgb(217, 168, 255),
        Color.rgb(255, 240, 179),
        Color.rgb(134, 239, 172),
        Color.rgb(255, 153, 153),
    )

    // ── Pre-allocated Paints ─────────────────────────────────────────────────
    private val bgPaint = Paint().apply {
        shader = RadialGradient(
            screenWidth / 2, screenHeight / 2, screenHeight * 0.8f,
            intArrayOf(Color.rgb(30, 10, 58), Color.rgb(21, 5, 41)),
            floatArrayOf(0f, 1f), Shader.TileMode.CLAMP,
        )
    }

    private val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            centerX, centerY, outerRadius,
            Color.rgb(21, 5, 41), Color.rgb(42, 16, 80),
            Shader.TileMode.CLAMP,
        )
    }

    private val highwayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(60, 80, 225, 249)
    }

    private val laneDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(38, 81, 64, 103)
        strokeWidth = 1.5f
    }

    private val speedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(30, 80, 225, 249)
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val hitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 225, 249)
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val hitLineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 80, 225, 249)
        strokeWidth = 18f
        style = Paint.Style.STROKE
    }

    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(120, 240, 223, 255)
    }
    private val noteGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 240, 223, 255)
    }

    private val holdPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val holdBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val laneHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val hitCorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hitHaloPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hitSparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(80, 225, 249)
        strokeWidth = 4f
    }

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
    private var tintedSmokeBitmaps: Array<Array<Bitmap>> = emptyArray()

    private val progressBgPaint = Paint().apply { color = Color.rgb(21, 5, 41) }
    private val progressFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val powerBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarFlamePaint = Paint(Paint.ANTI_ALIAS_FLAG)

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

    private val overpressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 82, 82)
        textSize = 36f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // ── Pre-allocated Paths & Rects ──────────────────────────────────────────
    private val arcPath = Path()
    private val laneHighlightPath = Path()
    private val holdTrailPath = Path()
    private val rectF = RectF()
    private val shaderMatrix = Matrix()

    // Pre-cached shaders
    private val laneNoteShaders = Array(laneCount) { i ->
        RadialGradient(
            0f, 0f, 1f,
            intArrayOf(Color.argb(240, 240, 223, 255), laneColors[i], Color.rgb(21, 5, 41)),
            floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
        )
    }

    private val fretSocketShaders = Array(laneCount) { i ->
        val pos = fretPosition(i)
        RadialGradient(pos.first, pos.second, fretButtonRadius,
            intArrayOf(Color.rgb(42, 16, 80), Color.rgb(21, 5, 41)),
            floatArrayOf(0.8f, 1f), Shader.TileMode.CLAMP)
    }

    private val fretGlowShaders = Array(laneCount) { i ->
        val pos = fretPosition(i)
        RadialGradient(pos.first, pos.second, fretButtonRadius * 0.7f,
            intArrayOf(Color.argb(200, 240, 223, 255), laneColors[i], Color.rgb(21, 5, 41)),
            floatArrayOf(0.1f, 0.6f, 1f), Shader.TileMode.CLAMP)
    }

    private val fretPressedGlowShaders = Array(laneCount) { i ->
        val pos = fretPosition(i)
        RadialGradient(pos.first, pos.second, fretButtonRadius * 0.9f,
            intArrayOf(Color.argb(200, 240, 223, 255), laneColors[i], Color.rgb(21, 5, 41)),
            floatArrayOf(0.1f, 0.6f, 1f), Shader.TileMode.CLAMP)
    }

    private val progressGradient = LinearGradient(
        0f, 0f, screenWidth, 0f,
        Color.rgb(80, 225, 249), Color.rgb(196, 127, 255),
        Shader.TileMode.CLAMP,
    )

    init {
        whiteSmokeBitmaps = createWhiteSmokeBitmaps()
        tintedSmokeBitmaps = createTintedSmokeBitmaps(whiteSmokeBitmaps, laneColors)
    }

    // ── IGameRenderer Implementation ─────────────────────────────────────────

    override fun getHitLineY(): Float = hitLineY
    override fun getHighwayBottomLeft(): Float = highwayBottomLeft
    override fun getHighwayBottomWidth(): Float = highwayBottomWidth
    override fun getStrikeLineY(): Float {
        // Return the Y of the center fret button (lane 2, the middle lane)
        return fretPosition(2).second
    }
    override fun getLaneCenterXAtStrikeline(laneIndex: Int): Float = fretPosition(laneIndex).first
    override fun getLaneColor(laneIndex: Int): Int = laneColors[laneIndex.coerceIn(0, laneCount - 1)]
    override fun getFretPosition(laneIndex: Int): Pair<Float, Float> = fretPosition(laneIndex)
    override fun getParticleDirection(laneIndex: Int): Pair<Float, Float> {
        val angle = laneAngle(laneIndex)
        return Pair(-sin(angle), -cos(angle))
    }

    // ── Arc Geometry Helpers ─────────────────────────────────────────────────

    /** Angle (in radians, 0=straight down) for a given lane center */
    private fun laneAngle(laneIndex: Int): Float {
        // Lanes go left to right: -halfSpan ... +halfSpan
        val step = totalSpanRad / laneCount
        return -halfSpanRad + step * laneIndex + step / 2f
    }

    /** Angle for the left edge of a lane */
    private fun laneLeftAngle(laneIndex: Int): Float {
        val step = totalSpanRad / laneCount
        return -halfSpanRad + step * laneIndex
    }

    /** Angle for the right edge of a lane */
    private fun laneRightAngle(laneIndex: Int): Float {
        val step = totalSpanRad / laneCount
        return -halfSpanRad + step * (laneIndex + 1)
    }

    /** Convert a linear progress (0=top, 1=hit line) to a radius with perspective curve */
    private fun progressToRadius(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return innerRadius + curved * (outerRadius - innerRadius)
    }

    /** Scale factor for a given progress (matches highway renderer) */
    private fun perspectiveScale(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return 0.08f + 0.92f * curved
    }

    /** Convert (angle, radius) to screen (x, y). Angle 0 = straight down. */
    private fun polarToScreen(angle: Float, radius: Float): Pair<Float, Float> {
        val x = centerX + radius * sin(angle)
        val y = centerY + radius * cos(angle)
        return Pair(x, y)
    }

    /** Get screen position of a note at a given lane and progress */
    private fun notePosition(laneIndex: Int, progress: Float): Pair<Float, Float> {
        val r = progressToRadius(progress)
        val angle = laneAngle(laneIndex)
        return polarToScreen(angle, r)
    }

    /** Get the position of a fret button (at the hit line) */
    private fun fretPosition(laneIndex: Int): Pair<Float, Float> {
        val angle = laneAngle(laneIndex)
        return polarToScreen(angle, outerRadius)
    }

    /** Note progress from y position (same contract as highway renderer) */
    private fun noteProgress(activeNote: ActiveNote): Float {
        return (activeNote.y / hitLineY).coerceIn(-0.2f, 1.5f)
    }

    // ── Convert angle + radius to RectF for drawArc ──────────────────────────
    // Android's drawArc uses degrees from 3-o'clock, counter-clockwise.
    // Our angle system: 0 = straight down, positive = clockwise (right).
    // Convert: android_degrees = 90 - degrees_from_down
    private fun arcAngleToDegrees(angle: Float): Float {
        return 90f - Math.toDegrees(angle.toDouble()).toFloat()
    }

    // ═══════════════════ MAIN RENDER ═══════════════════

    override fun render(canvas: Canvas, state: GameState) {
        val frameTimeMs = state.frameTimeMs

        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)
        drawArcHighway(canvas, frameTimeMs)
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

    // ═══════════════════ ARC HIGHWAY ═══════════════════

    private fun drawArcHighway(canvas: Canvas, frameTimeMs: Long) {
        // Draw filled fan/sector shape
        arcPath.reset()
        val innerLeft = polarToScreen(-halfSpanRad, innerRadius)
        arcPath.moveTo(innerLeft.first, innerLeft.second)

        // Inner arc (top): left to right
        val innerArcRect = RectF(
            centerX - innerRadius, centerY - innerRadius,
            centerX + innerRadius, centerY + innerRadius,
        )
        val sweepDeg = Math.toDegrees(totalSpanRad.toDouble()).toFloat()
        // Inner arc: left (155°) → right (25°), counterclockwise = negative sweep
        val innerStartDeg = arcAngleToDegrees(-halfSpanRad) // 155° (left edge)
        arcPath.arcTo(innerArcRect, innerStartDeg, -sweepDeg, false)

        // Line to outer arc right edge
        val outerRight = polarToScreen(halfSpanRad, outerRadius)
        arcPath.lineTo(outerRight.first, outerRight.second)

        // Outer arc (bottom): right (25°) → left (155°), clockwise = positive sweep
        val outerArcRect = RectF(
            centerX - outerRadius, centerY - outerRadius,
            centerX + outerRadius, centerY + outerRadius,
        )
        val outerStartDeg = arcAngleToDegrees(halfSpanRad) // 25° (right edge)
        arcPath.arcTo(outerArcRect, outerStartDeg, sweepDeg, false)

        arcPath.close()

        canvas.drawPath(arcPath, highwayPaint)
        canvas.drawPath(arcPath, highwayBorderPaint)

        // Lane dividers: radial lines from inner to outer radius
        for (i in 1 until laneCount) {
            val angle = laneLeftAngle(i)
            val inner = polarToScreen(angle, innerRadius)
            val outer = polarToScreen(angle, outerRadius)
            canvas.drawLine(inner.first, inner.second, outer.first, outer.second, laneDividerPaint)
        }

        // Speed lines: concentric arcs
        val scrollOffset = (frameTimeMs % 1000) / 1000f
        for (i in 0..10) {
            val baseP = i / 10f
            var p = baseP + scrollOffset * 0.1f
            if (p > 1f) p -= 1f

            val r = progressToRadius(p)
            val alpha = (sin(p * Math.PI) * 60).toInt().coerceIn(0, 60)
            speedLinePaint.alpha = alpha

            val speedRect = RectF(
                centerX - r, centerY - r, centerX + r, centerY + r,
            )
            val sStart = arcAngleToDegrees(halfSpanRad)
            canvas.drawArc(speedRect, sStart, sweepDeg, false, speedLinePaint)
        }
    }

    // ═══════════════════ STRIKELINE ═══════════════════

    private fun drawStrikeLine(canvas: Canvas, frameTimeMs: Long) {
        val pulse = (sin(frameTimeMs * 0.01).toFloat() + 1f) / 2f
        val sweepDeg = Math.toDegrees(totalSpanRad.toDouble()).toFloat()
        val startDeg = arcAngleToDegrees(halfSpanRad)

        val strikeRect = RectF(
            centerX - outerRadius, centerY - outerRadius,
            centerX + outerRadius, centerY + outerRadius,
        )

        hitLineGlowPaint.alpha = (80 + pulse * 120).toInt()
        canvas.drawArc(strikeRect, startDeg, sweepDeg, false, hitLineGlowPaint)

        hitLinePaint.strokeWidth = 4f + pulse * 2f
        canvas.drawArc(strikeRect, startDeg, sweepDeg, false, hitLinePaint)

        // End caps
        val leftPos = polarToScreen(-halfSpanRad, outerRadius)
        val rightPos = polarToScreen(halfSpanRad, outerRadius)
        buttonPaint.color = Color.rgb(80, 225, 249)
        buttonPaint.shader = null
        canvas.drawCircle(leftPos.first, leftPos.second, 8f, buttonPaint)
        canvas.drawCircle(rightPos.first, rightPos.second, 8f, buttonPaint)
    }

    // ═══════════════════ FRET BUTTONS ═══════════════════

    private fun drawFretButtons(canvas: Canvas, state: GameState) {
        for (i in 0 until laneCount) {
            val pos = fretPosition(i)
            val cx = pos.first
            val cy = pos.second
            val isPressed = (i + 1) in state.pressedLanes
            val color = laneColors[i]
            val radius = fretButtonRadius

            buttonPaint.shader = fretSocketShaders[i]
            canvas.drawCircle(cx, cy, radius, buttonPaint)

            val glowRadius = if (isPressed) radius * 0.9f else radius * 0.7f
            val alpha = if (isPressed) 255 else 100
            buttonPaint.shader = if (isPressed) fretPressedGlowShaders[i] else fretGlowShaders[i]
            buttonPaint.alpha = alpha
            canvas.drawCircle(cx, cy, glowRadius, buttonPaint)
            buttonPaint.shader = null
            buttonPaint.alpha = 255

            if (isPressed) {
                buttonGlowPaint.color = color
                buttonGlowPaint.alpha = 150
                canvas.drawCircle(cx, cy, radius * 1.4f, buttonGlowPaint)
            }
        }
    }

    // ═══════════════════ NOTES ═══════════════════

    private fun drawNote(canvas: Canvas, activeNote: ActiveNote) {
        val note = activeNote.note
        val laneIndex = note.lane - 1
        if (laneIndex !in 0 until laneCount) return

        val progress = noteProgress(activeNote)
        if (progress < -0.05f || progress > 1.3f) return

        val pos = notePosition(laneIndex, progress)
        val screenX = pos.first
        val screenY = pos.second
        val scale = perspectiveScale(progress)
        val glowColor = laneGlowColors[laneIndex]

        val baseRadius = fretButtonRadius * 0.85f
        val radius = baseRadius * scale

        if (radius < 2f) return

        // Proximity glow
        val distToStrike = abs(progress - 1f)
        if (distToStrike < 0.35f) {
            val intensity = 1f - distToStrike / 0.35f
            noteGlowPaint.color = glowColor
            noteGlowPaint.alpha = (intensity * 220).toInt()
            canvas.drawCircle(screenX, screenY, radius * 1.7f, noteGlowPaint)
        }

        // Gem body
        val noteShader = laneNoteShaders[laneIndex]
        shaderMatrix.reset()
        shaderMatrix.setScale(radius * 1.2f, radius * 1.2f)
        shaderMatrix.postTranslate(screenX - radius * 0.3f, screenY - radius * 0.3f)
        noteShader.setLocalMatrix(shaderMatrix)
        notePaint.shader = noteShader
        canvas.drawCircle(screenX, screenY, radius, notePaint)
        notePaint.shader = null

        // Rim
        noteRimPaint.strokeWidth = 3f * scale
        canvas.drawCircle(screenX, screenY, radius, noteRimPaint)

        // Center dot
        canvas.drawCircle(screenX, screenY, radius * 0.3f, noteCenterPaint)
    }

    // ═══════════════════ HOLD TRAILS ═══════════════════

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

        // Build path along the arc for this lane
        for (i in 0..steps) {
            val t = tailProgress + (headProgress - tailProgress) * i / steps
            val tc = t.coerceIn(0f, 1.2f)
            val pos = notePosition(laneIndex, tc)
            val s = perspectiveScale(tc)
            val halfW = trailHalfWidth * s
            // Offset perpendicular to the radial direction
            val angle = laneAngle(laneIndex)
            val perpX = cos(angle) // perpendicular to radial in our coord system
            val perpY = -sin(angle)
            if (i == 0) holdTrailPath.moveTo(pos.first - perpX * halfW, pos.second - perpY * halfW)
            else holdTrailPath.lineTo(pos.first - perpX * halfW, pos.second - perpY * halfW)
        }
        for (i in steps downTo 0) {
            val t = tailProgress + (headProgress - tailProgress) * i / steps
            val tc = t.coerceIn(0f, 1.2f)
            val pos = notePosition(laneIndex, tc)
            val s = perspectiveScale(tc)
            val halfW = trailHalfWidth * s
            val angle = laneAngle(laneIndex)
            val perpX = cos(angle)
            val perpY = -sin(angle)
            holdTrailPath.lineTo(pos.first + perpX * halfW, pos.second + perpY * halfW)
        }
        holdTrailPath.close()

        holdPaint.shader = null
        holdPaint.style = Paint.Style.FILL
        holdPaint.color = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
        canvas.drawPath(holdTrailPath, holdPaint)

        val pulse = ((sin(frameTimeMs * 0.006).toFloat() + 1f) / 2f * 70 + 50).toInt()
        holdBorderPaint.color = Color.argb(
            pulse, Color.red(color), Color.green(color), Color.blue(color),
        )
        canvas.drawPath(holdTrailPath, holdBorderPaint)
    }

    // ═══════════════════ LANE HIGHLIGHTS ═══════════════════

    private fun drawLaneHighlights(canvas: Canvas, state: GameState) {
        for (lane in state.pressedLanes) {
            val laneIndex = lane - 1
            if (laneIndex !in 0 until laneCount) continue
            val color = laneColors[laneIndex]

            laneHighlightPath.reset()
            val steps = 16
            val leftAngle = laneLeftAngle(laneIndex)
            val rightAngle = laneRightAngle(laneIndex)

            // Left edge: inner to outer
            for (i in 0..steps) {
                val p = i.toFloat() / steps
                val r = progressToRadius(p)
                val pos = polarToScreen(leftAngle, r)
                if (i == 0) laneHighlightPath.moveTo(pos.first, pos.second)
                else laneHighlightPath.lineTo(pos.first, pos.second)
            }
            // Outer arc: left to right (counterclockwise in Android = negative sweep)
            val outerRect = RectF(
                centerX - outerRadius, centerY - outerRadius,
                centerX + outerRadius, centerY + outerRadius,
            )
            val laneSweepDeg = Math.toDegrees((rightAngle - leftAngle).toDouble()).toFloat()
            val outerArcStart = arcAngleToDegrees(leftAngle) // left edge of this lane
            laneHighlightPath.arcTo(outerRect, outerArcStart, -laneSweepDeg, false)

            // Right edge: outer to inner
            for (i in steps downTo 0) {
                val p = i.toFloat() / steps
                val r = progressToRadius(p)
                val pos = polarToScreen(rightAngle, r)
                laneHighlightPath.lineTo(pos.first, pos.second)
            }
            // Inner arc: right to left (clockwise in Android = positive sweep)
            val innerRect = RectF(
                centerX - innerRadius, centerY - innerRadius,
                centerX + innerRadius, centerY + innerRadius,
            )
            val innerArcStart = arcAngleToDegrees(rightAngle) // right edge of this lane
            laneHighlightPath.arcTo(innerRect, innerArcStart, laneSweepDeg, false)

            laneHighlightPath.close()

            laneHighlightPaint.shader = null
            laneHighlightPaint.color = Color.argb(30, Color.red(color), Color.green(color), Color.blue(color))
            canvas.drawPath(laneHighlightPath, laneHighlightPaint)
        }
    }

    // ═══════════════════ HIT EFFECTS ═══════════════════

    private fun drawHitEffects(canvas: Canvas, state: GameState, frameTimeMs: Long) {
        for (effect in state.hitEffects) {
            val age = frameTimeMs - effect.createdAt
            if (age > 400) continue

            val progress = age / 400f
            val laneIndex = effect.lane - 1
            if (laneIndex !in 0 until laneCount) continue

            val pos = fretPosition(laneIndex)
            val cx = pos.first
            val cy = pos.second

            if (effect.result == HitResult.MISS) {
                val missAlpha = ((1f - progress) * 255).toInt()
                overpressPaint.alpha = missAlpha
                val textY = cy - 60f - progress * 40f
                canvas.drawText("X", cx, textY, overpressPaint)
                continue
            }

            val burstSize = fretButtonRadius * (1f + progress * 2f)
            val alpha = ((1f - progress) * 255).toInt()

            hitCorePaint.color = Color.rgb(240, 223, 255)
            hitCorePaint.alpha = alpha
            hitCorePaint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, burstSize * 0.4f, hitCorePaint)

            val haloColor = if (effect.result == HitResult.PERFECT) {
                Color.rgb(80, 225, 249)
            } else {
                Color.rgb(196, 127, 255)
            }
            hitHaloPaint.color = haloColor
            hitHaloPaint.alpha = (alpha * 0.7f).toInt()
            canvas.drawCircle(cx, cy, burstSize * 0.8f, hitHaloPaint)

            for (i in 0..4) {
                val sparkOffsetX = effect.sparkOffsetsX[i] * burstSize
                val sparkOffsetY = progress * 150f + effect.sparkOffsetsY[i] * 50f
                canvas.drawPoint(cx + sparkOffsetX, cy - sparkOffsetY, hitSparkPaint)
            }

            val textY = cy - 100f - progress * 50f
            hitResultTextPaint.color = Color.rgb(240, 223, 255)
            val text = when (effect.result) {
                HitResult.PERFECT -> "PERFECT!"
                HitResult.GREAT -> "GREAT"
                HitResult.GOOD -> "GOOD"
                HitResult.MISS -> "MISS"
            }
            canvas.drawText(text, cx, textY, hitResultTextPaint)
        }
    }

    // ═══════════════════ PARTICLES ═══════════════════

    private fun drawParticles(canvas: Canvas, state: GameState) {
        for (p in state.particles) {
            val alpha = (p.life * 255).toInt().coerceIn(0, 255)
            if (alpha <= 0) continue

            val laneIdx = p.laneIndex.coerceIn(0, laneCount - 1)
            val texIdx = p.textureIndex.coerceIn(0, SMOKE_VARIANT_COUNT - 1)

            val halfSize = p.size
            smokeRectF.set(p.x - halfSize, p.y - halfSize, p.x + halfSize, p.y + halfSize)

            smokePaint.alpha = alpha
            canvas.drawBitmap(tintedSmokeBitmaps[laneIdx][texIdx], null, smokeRectF, smokePaint)

            smokeCoreOverlayPaint.alpha = (alpha * 0.4f).toInt().coerceIn(0, 255)
            canvas.drawBitmap(whiteSmokeBitmaps[texIdx], null, smokeRectF, smokeCoreOverlayPaint)
        }
    }

    // ═══════════════════ SMOKE BITMAP GENERATION ═══════════════════

    private fun createWhiteSmokeBitmaps(): Array<Bitmap> {
        val size = SMOKE_BITMAP_SIZE
        val center = size / 2f
        return Array(SMOKE_VARIANT_COUNT) { variant ->
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val radius = when (variant) {
                0 -> size * 0.35f; 1 -> size * 0.40f; 2 -> size * 0.45f; 3 -> size * 0.48f; else -> size * 0.40f
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
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP,
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
                tintPaint.xfermode = null
                c.drawBitmap(base, 0f, 0f, tintPaint)
                tintPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                tintPaint.color = colors[colorIdx]
                c.drawRect(0f, 0f, base.width.toFloat(), base.height.toFloat(), tintPaint)
                tintPaint.xfermode = null
                tinted
            }
        }
    }

    // ═══════════════════ HUD (same as highway) ═══════════════════

    private fun drawHUD(canvas: Canvas, state: GameState) {
        val padding = 20f
        val scoreX = screenWidth - padding
        val scoreY = screenHeight * 0.05f + 40f
        canvas.drawText("${state.score}", scoreX, scoreY, scoreTextPaint)
        canvas.drawText("SCORE", scoreX, scoreY + 17f, scoreLabelPaint)

        if (state.comboMultiplier > 1.0f) {
            multiplierTextPaint.color = when {
                state.comboMultiplier >= 4f -> Color.rgb(255, 231, 146)
                state.comboMultiplier >= 3f -> Color.rgb(196, 127, 255)
                state.comboMultiplier >= 2f -> Color.rgb(80, 225, 249)
                else -> Color.rgb(240, 223, 255)
            }
            canvas.drawText("${state.comboMultiplier.toInt()}x", scoreX, scoreY + 50f, multiplierTextPaint)
        }

        if (state.combo > 2) {
            val comboX = screenWidth / 2
            // Place combo text above the arc center area
            val comboY = centerY + innerRadius * 0.4f

            val comboSize = when {
                state.combo >= 100 -> 46f
                state.combo >= 50 -> 40f
                state.combo >= 20 -> 34f
                else -> 28f
            }

            if (state.combo >= 20) {
                val glowColor = when {
                    state.combo >= 100 -> Color.rgb(255, 231, 146)
                    state.combo >= 50 -> Color.rgb(196, 127, 255)
                    else -> Color.rgb(80, 225, 249)
                }
                comboGlowTextPaint.color = glowColor
                comboGlowTextPaint.alpha = 90
                comboGlowTextPaint.textSize = comboSize + 4f
                canvas.drawText("${state.combo}", comboX, comboY, comboGlowTextPaint)
            }

            val comboColor = when {
                state.combo >= 100 -> Color.rgb(255, 231, 146)
                state.combo >= 50 -> Color.rgb(196, 127, 255)
                state.combo >= 20 -> Color.rgb(80, 225, 249)
                else -> Color.rgb(240, 223, 255)
            }
            comboTextPaint.color = comboColor
            comboTextPaint.textSize = comboSize
            canvas.drawText("${state.combo}", comboX, comboY, comboTextPaint)

            canvas.drawText("NOTE STREAK", comboX, comboY + 16f, streakLabelPaint)
        }
    }

    // ═══════════════════ POWER BAR (same as highway) ═══════════════════

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
        powerBarBgPaint.color = Color.argb(60, 255, 231, 146)
        canvas.drawLine(barLeft + 2f, tierLineY, barRight - 2f, tierLineY, powerBarBgPaint)

        if (progress > 0.001f) {
            val fillHeight = barHeight * progress
            val fillTop = barBottom - fillHeight

            val fillColor = when {
                progress >= 0.7f -> Color.rgb(255, 231, 146)
                progress >= 0.3f -> Color.rgb(196, 127, 255)
                else -> Color.rgb(80, 225, 249)
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
                barCenterX, barBottom + 13f + i * 10f,
                verticalLabelPaint,
            )
        }
    }

    // ═══════════════════ PROGRESS BAR ═══════════════════

    private fun drawProgressBar(canvas: Canvas, state: GameState) {
        val barHeight = 5f
        canvas.drawRect(0f, 0f, screenWidth, barHeight, progressBgPaint)

        if (state.songDurationMs > 0) {
            val progress = (state.currentTimeMs.toFloat() / state.songDurationMs).coerceIn(0f, 1f)
            val fillWidth = screenWidth * progress
            progressFillPaint.shader = progressGradient
            canvas.drawRect(0f, 0f, fillWidth, barHeight, progressFillPaint)
            progressFillPaint.shader = null
        }
    }

    // ═══════════════════ COUNTDOWN ═══════════════════

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

    // ═══════════════════ PAUSE OVERLAY ═══════════════════

    private fun drawPauseOverlay(canvas: Canvas) {
        overlayPaint.alpha = 190
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlayPaint)

        canvas.drawText("PAUSED", screenWidth / 2, screenHeight / 2 - 20f, pauseTitlePaint)
        canvas.drawText("Tap to resume", screenWidth / 2, screenHeight / 2 + 30f, pauseSubtitlePaint)
    }

    // ── Arc-specific touch helper ────────────────────────────────────────────
    /**
     * Given a touch (x, y), returns the lane number (1-based) or -1 if outside the arc highway.
     * This is used by InputHandler in arc mode.
     */
    fun getLaneFromTouch(x: Float, y: Float): Int {
        val dx = x - centerX
        val dy = y - centerY
        val r = sqrt(dx * dx + dy * dy)
        if (r < innerRadius * 0.5f || r > outerRadius * 1.15f) return -1

        // Angle from straight-down (our convention: 0 = down, positive = right)
        val angle = atan2(dx, dy) // atan2(x, y) gives angle from Y-axis
        if (angle < -halfSpanRad * 1.1f || angle > halfSpanRad * 1.1f) return -1

        val step = totalSpanRad / laneCount
        val laneIndex = ((angle + halfSpanRad) / step).toInt()
        return (laneIndex + 1).coerceIn(1, laneCount)
    }

    /** Returns the center X/Y of the arc (for InputHandler geometry). */
    fun getArcCenterX(): Float = centerX
    fun getArcCenterY(): Float = centerY
    fun getInnerRadius(): Float = innerRadius
    fun getOuterRadius(): Float = outerRadius
    fun getHalfSpanRad(): Float = halfSpanRad
}
