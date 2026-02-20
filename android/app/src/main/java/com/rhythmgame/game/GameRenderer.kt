package com.rhythmgame.game

import android.graphics.*
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class GameRenderer(
    private val screenWidth: Float,
    private val screenHeight: Float,
) {
    private val laneCount = 5

    // --- Highway Geometry (3D perspective) ---
    private val hitLineY = screenHeight * 0.8f
    private val highwayBottomY = hitLineY
    private val highwayTopY = screenHeight * 0.12f
    private val highwayBottomWidth = screenWidth * 0.92f
    private val highwayTopWidth = screenWidth * 0.08f
    private val highwayBottomLeft = (screenWidth - highwayBottomWidth) / 2f
    private val highwayBottomRight = highwayBottomLeft + highwayBottomWidth
    private val highwayTopLeft = (screenWidth - highwayTopWidth) / 2f
    private val highwayTopRight = highwayTopLeft + highwayTopWidth
    private val fretButtonRadius = highwayBottomWidth / laneCount / 2f * 0.78f
    private val perspectiveExponent = 2.5f

    // Power Bar geometry
    private val powerBarRight = screenWidth - 10f
    private val powerBarBarWidth = 20f
    private val powerBarLeft = powerBarRight - powerBarBarWidth
    private val powerBarTop = screenHeight * 0.25f
    private val powerBarBottom = screenHeight * 0.72f
    private val powerBarHeight = powerBarBottom - powerBarTop

    // Lane colors (Guitar Hero Classic: Green, Red, Yellow, Blue, Orange)
    private val laneColors = intArrayOf(
        Color.rgb(0, 200, 83),     // Toxic Green
        Color.rgb(229, 57, 53),    // Crimson Red
        Color.rgb(255, 214, 0),    // Gold Yellow
        Color.rgb(41, 121, 255),   // Electric Blue
        Color.rgb(255, 109, 0),    // Magma Orange
    )

    private val laneGlowColors = intArrayOf(
        Color.rgb(105, 240, 174),
        Color.rgb(255, 138, 128),
        Color.rgb(255, 255, 141),
        Color.rgb(128, 216, 255),
        Color.rgb(255, 171, 64),
    )

    // --- Pre-allocated Paints ---
    // Background: Dark atmospheric gradient
    private val bgPaint = Paint().apply {
        shader = RadialGradient(
            screenWidth / 2, screenHeight / 2,
            screenHeight * 0.8f,
            intArrayOf(Color.rgb(40, 10, 15), Color.BLACK),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
    }

    private val highwayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Metallic Highway Borders
    private val highwayBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        shader = LinearGradient(
            0f, 0f, screenWidth, 0f,
            intArrayOf(Color.GRAY, Color.WHITE, Color.DKGRAY),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.MIRROR
        )
    }

    private val laneDividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 200, 200, 200)
        strokeWidth = 2f
    }

    private val speedLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 255, 100, 50) // Fire colored speed lines
        strokeWidth = 2f
    }

    // Electric Strike Line
    private val hitLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val hitLineGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.CYAN
        strokeWidth = 12f
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    // Note Gems
    private val notePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteRimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.rgb(220, 220, 220) // Silver rim
    }

    private val noteGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
    }

    private val noteHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val noteOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val holdPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val holdBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // Fire/Spark Effects
    private val hitEffectPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(10f, 0f, 0f, Color.RED) // Red shadow for rock feel
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 32f
    }

    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val laneHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressBgPaint = Paint().apply { color = Color.rgb(25, 25, 40) }
    private val progressFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val comboPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val comboGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }
    private val rockMeterPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val powerBarFlamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }

    // --- Pre-allocated Paths ---
    private val highwayPath = Path()
    private val laneHighlightPath = Path()
    private val holdTrailPath = Path()

    private val rectF = RectF()

    init {
        // Pre-compute the static highway path
        highwayPath.apply {
            moveTo(highwayTopLeft, highwayTopY)
            lineTo(highwayTopRight, highwayTopY)
            lineTo(highwayBottomRight, highwayBottomY)
            lineTo(highwayBottomLeft, highwayBottomY)
            close()
        }
    }

    // --- Public getters for GameEngine ---
    fun getHitLineY(): Float = hitLineY
    fun getHighwayBottomLeft(): Float = highwayBottomLeft
    fun getHighwayBottomWidth(): Float = highwayBottomWidth
    fun getStrikeLineY(): Float = highwayBottomY

    fun getLaneCenterXAtStrikeline(laneIndex: Int): Float = laneCenterX(laneIndex, 1.0f)

    // --- Perspective transformation functions ---

    /** Maps linear progress [0..1] to screen Y with perspective foreshortening */
    private fun perspectiveY(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return highwayTopY + curved * (highwayBottomY - highwayTopY)
    }

    /** Scale factor at a given progress (small at far, full at near) */
    private fun perspectiveScale(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return 0.08f + 0.92f * curved
    }

    /** Left X boundary of highway at a given progress */
    private fun highwayLeftAtProgress(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return highwayTopLeft + curved * (highwayBottomLeft - highwayTopLeft)
    }

    /** Right X boundary of highway at a given progress */
    private fun highwayRightAtProgress(progress: Float): Float {
        val curved = progress.coerceIn(0f, 1.5f).pow(perspectiveExponent)
        return highwayTopRight + curved * (highwayBottomRight - highwayTopRight)
    }

    /** Center X for a lane (0-based) at a given progress */
    private fun laneCenterX(laneIndex: Int, progress: Float): Float {
        val left = highwayLeftAtProgress(progress)
        val right = highwayRightAtProgress(progress)
        val laneWidth = (right - left) / laneCount
        return left + laneWidth * laneIndex + laneWidth / 2f
    }

    /** Convert ActiveNote.y back to linear progress (0..1) */
    private fun noteProgress(activeNote: ActiveNote): Float {
        return (activeNote.y / hitLineY).coerceIn(-0.2f, 1.5f)
    }

    // ========== MAIN RENDER ==========

    fun render(canvas: Canvas, state: GameState) {
        // 1. Dark Atmospheric Background
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        // 2. Highway (The Fretboard)
        drawHighway(canvas)

        // 3. Lane Highlights (Pressed fret columns)
        drawLaneHighlights(canvas, state)

        // 4. Hold Trails
        for (activeNote in state.activeNotes) {
            if (activeNote.isHit && activeNote.note.isTap) continue
            if (activeNote.note.isHold) drawHoldTrail(canvas, activeNote)
        }

        // 5. Strike Line (The Target)
        drawStrikeLine(canvas)

        // 6. Fret Buttons (The Inputs)
        drawFretButtons(canvas, state)

        // 7. Notes (The Gems)
        for (activeNote in state.activeNotes) {
            if (activeNote.isHit && activeNote.note.isTap) continue
            drawNote(canvas, activeNote)
        }

        // 8. Hit Effects (Fire/Sparks)
        drawHitEffects(canvas, state)

        // 9. UI Elements
        drawRockMeter(canvas, state)
        drawPowerBar(canvas, state)
        drawHUD(canvas, state)
        drawProgressBar(canvas, state)

        // 10. Overlays
        when (state.phase) {
            GamePhase.COUNTDOWN -> drawCountdown(canvas, state.countdownValue)
            GamePhase.PAUSED -> drawPauseOverlay(canvas)
            else -> {}
        }
    }

    // ========== HIGHWAY ==========

    private fun drawHighway(canvas: Canvas) {
        // Highway surface: Dark asphalt/metal texture
        highwayPaint.shader = LinearGradient(
            screenWidth / 2, highwayTopY,
            screenWidth / 2, highwayBottomY,
            Color.rgb(20, 20, 25),   // Deep dark gray at top
            Color.rgb(40, 40, 50),   // Slightly lighter at bottom
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(highwayPath, highwayPaint)
        highwayPaint.shader = null

        // Metallic Borders
        canvas.drawPath(highwayPath, highwayBorderPaint)

        // Lane Dividers
        for (i in 1 until laneCount) {
            val topX = highwayTopLeft + (highwayTopRight - highwayTopLeft) * i / laneCount
            val bottomX = highwayBottomLeft + (highwayBottomRight - highwayBottomLeft) * i / laneCount
            canvas.drawLine(topX, highwayTopY, bottomX, highwayBottomY, laneDividerPaint)
        }

        // Moving Speed Lines (Road markings)
        val time = System.currentTimeMillis()
        val scrollOffset = (time % 1000) / 1000f // 0 to 1 loop
        
        for (i in 0..10) {
            val baseP = i / 10f
            var p = baseP + scrollOffset * 0.1f // Move forward
            if (p > 1f) p -= 1f
            
            // Only draw lines on the track
            val y = perspectiveY(p)
            val left = highwayLeftAtProgress(p)
            val right = highwayRightAtProgress(p)
            
            // Fade in/out
            val alpha = (sin(p * Math.PI) * 100).toInt().coerceIn(0, 100)
            speedLinePaint.alpha = alpha
            
            // Draw horizontal line
            canvas.drawLine(left, y, right, y, speedLinePaint)
        }
    }

    // ========== STRIKELINE ==========

    private fun drawStrikeLine(canvas: Canvas) {
        val y = highwayBottomY
        val left = highwayBottomLeft
        val right = highwayBottomRight
        val time = System.currentTimeMillis()
        
        // Pulsing electric effect
        val pulse = (sin(time * 0.01).toFloat() + 1f) / 2f
        
        // 1. Outer Glow (Electric Blue/Purple)
        hitLineGlowPaint.color = Color.rgb(0, 200, 255)
        hitLineGlowPaint.alpha = (100 + pulse * 100).toInt()
        canvas.drawLine(left, y, right, y, hitLineGlowPaint)
        
        // 2. Core Beam (White/Cyan)
        hitLinePaint.color = Color.rgb(200, 255, 255)
        hitLinePaint.alpha = 255
        hitLinePaint.strokeWidth = 4f + pulse * 2f
        canvas.drawLine(left, y, right, y, hitLinePaint)
        
        // 3. Side posts (Metallic anchors)
        buttonPaint.color = Color.LTGRAY
        buttonPaint.shader = null
        canvas.drawCircle(left, y, 10f, buttonPaint)
        canvas.drawCircle(right, y, 10f, buttonPaint)
    }

    // ========== FRET BUTTONS ==========

    private fun drawFretButtons(canvas: Canvas, state: GameState) {
        for (i in 0 until laneCount) {
            val centerX = laneCenterX(i, 1.0f)
            val centerY = highwayBottomY
            val isPressed = (i + 1) in state.pressedLanes
            val color = laneColors[i]
            val radius = fretButtonRadius

            // Base: Dark metallic socket
            buttonPaint.shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(Color.DKGRAY, Color.BLACK),
                floatArrayOf(0.8f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(centerX, centerY, radius, buttonPaint)

            // Inner Light: The actual button color
            val glowRadius = if (isPressed) radius * 0.9f else radius * 0.7f
            val alpha = if (isPressed) 255 else 100
            
            buttonPaint.shader = RadialGradient(
                centerX, centerY, glowRadius,
                intArrayOf(Color.WHITE, color, Color.BLACK), // Bright center
                floatArrayOf(0.1f, 0.6f, 1f),
                Shader.TileMode.CLAMP
            )
            buttonPaint.alpha = alpha
            canvas.drawCircle(centerX, centerY, glowRadius, buttonPaint)
            buttonPaint.shader = null
            buttonPaint.alpha = 255

            // Pressed Glow Halo
            if (isPressed) {
                noteGlowPaint.color = color
                noteGlowPaint.alpha = 150
                canvas.drawCircle(centerX, centerY, radius * 1.4f, noteGlowPaint)
                
                // Lightning spark
                hitEffectPaint.color = Color.WHITE
                hitEffectPaint.strokeWidth = 2f
                val sparkY = centerY - radius * 1.5f
                canvas.drawLine(centerX, centerY, centerX, sparkY, hitEffectPaint)
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

        // 1. Outer Glow (only when near strikeline)
        val distToStrike = abs(progress - 1f)
        if (distToStrike < 0.3f) {
            noteGlowPaint.color = glowColor
            noteGlowPaint.alpha = ((1f - distToStrike/0.3f) * 200).toInt()
            canvas.drawCircle(centerX, screenY, radius * 1.5f, noteGlowPaint)
        }

        // 2. Gem Body (Radial Gradient for 3D sphere look)
        notePaint.shader = RadialGradient(
            centerX - radius * 0.3f, screenY - radius * 0.3f, // Offset highlight
            radius * 1.2f,
            intArrayOf(Color.WHITE, color, Color.rgb(20, 20, 20)), // White -> Color -> Dark
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, screenY, radius, notePaint)
        notePaint.shader = null

        // 3. Metallic Rim (Silver ring around note)
        noteRimPaint.strokeWidth = 3f * scale
        noteRimPaint.color = Color.LTGRAY
        canvas.drawCircle(centerX, screenY, radius, noteRimPaint)
        
        // 4. Inner Ring (White center dot)
        notePaint.color = Color.WHITE
        notePaint.alpha = 180
        canvas.drawCircle(centerX, screenY, radius * 0.3f, notePaint)
        notePaint.alpha = 255
    }

    // ========== HOLD TRAILS ==========

    private fun drawHoldTrail(canvas: Canvas, activeNote: ActiveNote) {
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

        // Left edge: tail → head
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
        // Right edge: head → tail
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

        // Gradient fill
        val tailY = perspectiveY(tailProgress.coerceIn(0f, 1.2f))
        val headY = perspectiveY(headProgress.coerceIn(0f, 1.2f))
        holdPaint.style = Paint.Style.FILL
        holdPaint.shader = LinearGradient(
            0f, tailY, 0f, headY,
            Color.argb(35, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(150, Color.red(color), Color.green(color), Color.blue(color)),
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(holdTrailPath, holdPaint)
        holdPaint.shader = null

        // Pulsing border
        val pulse = ((sin(System.currentTimeMillis() * 0.006).toFloat() + 1f) / 2f * 70 + 50).toInt()
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

            // Left edge: top → bottom
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
            // Right edge: bottom → top
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

            laneHighlightPaint.shader = LinearGradient(
                0f, highwayTopY, 0f, highwayBottomY,
                Color.TRANSPARENT,
                Color.argb(55, Color.red(color), Color.green(color), Color.blue(color)),
                Shader.TileMode.CLAMP
            )
            canvas.drawPath(laneHighlightPath, laneHighlightPaint)
            laneHighlightPaint.shader = null
        }
    }

    // ========== PARTICLES ==========

    private fun drawParticles(canvas: Canvas, state: GameState) {
        for (p in state.particles) {
            val alpha = (p.life * 255).toInt().coerceIn(0, 255)
            particlePaint.color = p.color
            particlePaint.alpha = alpha
            canvas.drawCircle(p.x, p.y, p.size * p.life, particlePaint)
        }
    }

    // ========== HIT EFFECTS (Fire!) ==========

    private fun drawHitEffects(canvas: Canvas, state: GameState) {
        val now = System.currentTimeMillis()
        for (effect in state.hitEffects) {
            val age = now - effect.createdAt
            if (age > 400) continue // Short burst

            val progress = age / 400f // 0 to 1
            val laneIndex = effect.lane - 1
            if (laneIndex !in 0 until laneCount) continue
            
            val centerX = laneCenterX(laneIndex, 1.0f)
            val centerY = highwayBottomY

            val color = laneColors[laneIndex]

            // Skip miss effects
            if (effect.result == HitResult.MISS) continue
            
            // Fire Burst logic
            val burstSize = fretButtonRadius * (1f + progress * 2f)
            val alpha = ((1f - progress) * 255).toInt()
            
            // 1. Core explosion (White hot)
            hitEffectPaint.color = Color.WHITE
            hitEffectPaint.alpha = alpha
            canvas.drawCircle(centerX, centerY, burstSize * 0.4f, hitEffectPaint)
            
            // 2. Fire Halo (Orange/Red)
            hitEffectPaint.color = Color.rgb(255, 100, 0) // Fire orange
            hitEffectPaint.alpha = (alpha * 0.7f).toInt()
            canvas.drawCircle(centerX, centerY, burstSize * 0.8f, hitEffectPaint)
            
            // 3. Sparks (Rising)
            for (i in 0..4) {
                val sparkX = centerX + (Math.random() - 0.5) * burstSize
                val sparkY = centerY - progress * 150f - Math.random() * 50f
                hitEffectPaint.color = Color.YELLOW
                hitEffectPaint.strokeWidth = 4f
                canvas.drawPoint(sparkX.toFloat(), sparkY.toFloat(), hitEffectPaint)
            }
            
            // Result Text (Popup)
            val textY = centerY - 100f - progress * 50f
            smallTextPaint.textSize = 40f
            smallTextPaint.color = if (effect.result == HitResult.MISS) Color.RED else Color.WHITE
            smallTextPaint.textAlign = Paint.Align.CENTER
            smallTextPaint.setShadowLayer(5f, 0f, 0f, Color.BLACK)
            
            val text = when(effect.result) {
                HitResult.PERFECT -> "PERFECT!"
                HitResult.GREAT -> "GREAT"
                HitResult.GOOD -> "GOOD"
                HitResult.MISS -> "MISS"
            }
            canvas.drawText(text, centerX, textY, smallTextPaint)
        }
    }

    // ========== ROCK METER ==========

    private fun drawRockMeter(canvas: Canvas, state: GameState) {
        val meterLeft = 10f
        val meterWidth = 14f
        val meterTop = screenHeight * 0.25f
        val meterBottom = screenHeight * 0.72f
        val meterHeight = meterBottom - meterTop

        // Background track
        rockMeterPaint.style = Paint.Style.FILL
        rockMeterPaint.shader = null
        rockMeterPaint.color = Color.argb(70, 30, 30, 40)
        rectF.set(meterLeft, meterTop, meterLeft + meterWidth, meterBottom)
        canvas.drawRoundRect(rectF, 7f, 7f, rockMeterPaint)

        // Performance ratio
        val total = state.perfectCount + state.greatCount + state.goodCount + state.missCount
        val performanceRatio = if (total > 0) {
            (state.perfectCount * 1.0f + state.greatCount * 0.75f + state.goodCount * 0.5f) / total
        } else 0.5f

        val fillHeight = meterHeight * performanceRatio
        val fillTop = meterBottom - fillHeight

        val meterColor = when {
            performanceRatio >= 0.7f -> Color.rgb(76, 175, 80)
            performanceRatio >= 0.4f -> Color.rgb(255, 235, 59)
            else -> Color.rgb(244, 67, 54)
        }

        rockMeterPaint.shader = LinearGradient(
            meterLeft, fillTop, meterLeft, meterBottom,
            meterColor,
            Color.argb(160, Color.red(meterColor), Color.green(meterColor), Color.blue(meterColor)),
            Shader.TileMode.CLAMP
        )
        rectF.set(meterLeft, fillTop, meterLeft + meterWidth, meterBottom)
        canvas.drawRoundRect(rectF, 7f, 7f, rockMeterPaint)
        rockMeterPaint.shader = null

        // Needle
        rockMeterPaint.color = Color.WHITE
        rockMeterPaint.strokeWidth = 1.5f
        rockMeterPaint.style = Paint.Style.STROKE
        canvas.drawLine(meterLeft - 2f, fillTop, meterLeft + meterWidth + 2f, fillTop, rockMeterPaint)
        rockMeterPaint.style = Paint.Style.FILL

        // "ROCK" label
        smallTextPaint.textAlign = Paint.Align.CENTER
        smallTextPaint.textSize = 9f
        smallTextPaint.color = Color.argb(100, 255, 255, 255)
        val cx = meterLeft + meterWidth / 2
        canvas.drawText("R", cx, meterBottom + 13f, smallTextPaint)
        canvas.drawText("O", cx, meterBottom + 23f, smallTextPaint)
        canvas.drawText("C", cx, meterBottom + 33f, smallTextPaint)
        canvas.drawText("K", cx, meterBottom + 43f, smallTextPaint)
        smallTextPaint.textSize = 32f
        smallTextPaint.color = Color.WHITE
    }

    // ========== POWER BAR (Flame Bar) ==========

    private fun drawPowerBar(canvas: Canvas, state: GameState) {
        val barLeft = powerBarLeft
        val barRight = powerBarRight
        val barTop = powerBarTop
        val barBottom = powerBarBottom
        val barHeight = powerBarHeight
        val barCenterX = (barLeft + barRight) / 2f
        val progress = state.powerBarProgress.coerceIn(0f, 1f)
        val time = System.currentTimeMillis()

        // Background
        powerBarBgPaint.style = Paint.Style.FILL
        powerBarBgPaint.shader = null
        powerBarBgPaint.color = Color.argb(80, 20, 15, 10)
        rectF.set(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rectF, 6f, 6f, powerBarBgPaint)

        // Tier separator line at 50%
        val tierLineY = barBottom - barHeight * 0.5f
        powerBarBgPaint.color = Color.argb(80, 255, 150, 0)
        canvas.drawLine(barLeft + 2f, tierLineY, barRight - 2f, tierLineY, powerBarBgPaint)

        // Fill
        if (progress > 0.001f) {
            val fillHeight = barHeight * progress
            val fillTop = barBottom - fillHeight

            // Multi-color gradient: amber -> deep orange -> crimson
            powerBarFillPaint.shader = LinearGradient(
                barCenterX, barBottom, barCenterX, barTop,
                intArrayOf(
                    Color.rgb(255, 193, 7),
                    Color.rgb(255, 87, 34),
                    Color.rgb(255, 23, 68),
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP,
            )
            powerBarFillPaint.style = Paint.Style.FILL

            // Clip to fill area for clean rounded look
            canvas.save()
            canvas.clipRect(barLeft, fillTop, barRight, barBottom)
            rectF.set(barLeft, barTop, barRight, barBottom)
            canvas.drawRoundRect(rectF, 6f, 6f, powerBarFillPaint)
            canvas.restore()
            powerBarFillPaint.shader = null

            // Flame particles at fill top
            val flameCount = if (progress > 0.5f) 7 else 4
            for (i in 0 until flameCount) {
                val phase = time * 0.004f + i * 1.3f
                val flameX = barCenterX + sin(phase.toDouble()).toFloat() * powerBarBarWidth * 0.4f
                val flameY = fillTop - 3f - abs(sin((phase * 1.5f).toDouble())).toFloat() * 14f
                val flameSize = 2.5f + sin((phase * 0.7f).toDouble()).toFloat() * 1.5f
                val flameAlpha = (130 + (sin(phase.toDouble()) * 80).toInt()).coerceIn(0, 255)
                val flameColor = if (progress > 0.5f) {
                    Color.argb(flameAlpha, 255, 60, 0)
                } else {
                    Color.argb(flameAlpha, 255, 180, 0)
                }
                powerBarFlamePaint.color = flameColor
                canvas.drawCircle(flameX, flameY, flameSize, powerBarFlamePaint)
            }
        }

        // Border
        val borderColor: Int = when {
            state.powerActive -> {
                val pulse = (sin(time * 0.008).toFloat() + 1f) / 2f
                Color.argb((150 + (pulse * 105).toInt()).coerceIn(0, 255), 255, 200, 0)
            }
            progress >= 1f -> {
                val pulse = (sin(time * 0.005).toFloat() + 1f) / 2f
                Color.argb((100 + (pulse * 100).toInt()).coerceIn(0, 255), 255, 50, 0)
            }
            progress >= 0.5f -> Color.argb(120, 255, 100, 0)
            else -> Color.argb(50, 255, 150, 0)
        }

        powerBarBgPaint.style = Paint.Style.STROKE
        powerBarBgPaint.strokeWidth = if (state.powerActive) 3f else 1.5f
        powerBarBgPaint.color = borderColor
        rectF.set(barLeft, barTop, barRight, barBottom)
        canvas.drawRoundRect(rectF, 6f, 6f, powerBarBgPaint)
        powerBarBgPaint.style = Paint.Style.FILL

        // Active power glow
        if (state.powerActive) {
            val pulse = (sin(time * 0.006).toFloat() + 1f) / 2f
            powerBarFlamePaint.color = Color.argb(
                (40 + (pulse * 60).toInt()).coerceIn(0, 255), 255, 200, 0,
            )
            rectF.set(barLeft - 4f, barTop - 4f, barRight + 4f, barBottom + 4f)
            canvas.drawRoundRect(rectF, 8f, 8f, powerBarFlamePaint)
        }

        // Multiplier indicator
        if (state.powerActive) {
            val multText = "${state.powerMultiplier.toInt()}X"
            smallTextPaint.textAlign = Paint.Align.CENTER
            smallTextPaint.textSize = 18f
            smallTextPaint.color = Color.rgb(255, 215, 0)
            smallTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(multText, barCenterX, barTop - 10f, smallTextPaint)
        } else if (progress >= 0.5f) {
            val tier = if (progress >= 1f) 2 else 1
            val multText = if (tier == 2) "4X" else "2X"
            val pulse = (sin(time * 0.004).toFloat() + 1f) / 2f
            val alpha = (120 + (pulse * 135).toInt()).coerceIn(0, 255)
            smallTextPaint.textAlign = Paint.Align.CENTER
            smallTextPaint.textSize = 14f
            smallTextPaint.color = Color.argb(alpha, 255, 200, 0)
            smallTextPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(multText, barCenterX, barTop - 6f, smallTextPaint)
        }

        // "POWER" label below bar (vertical)
        smallTextPaint.textAlign = Paint.Align.CENTER
        smallTextPaint.textSize = 9f
        smallTextPaint.color = Color.argb(100, 255, 200, 150)
        smallTextPaint.typeface = Typeface.DEFAULT
        val labelChars = "POWER"
        for (i in labelChars.indices) {
            canvas.drawText(
                labelChars[i].toString(),
                barCenterX,
                barBottom + 13f + i * 10f,
                smallTextPaint,
            )
        }

        // Reset paint state
        smallTextPaint.textSize = 32f
        smallTextPaint.color = Color.WHITE
        smallTextPaint.alpha = 255
        smallTextPaint.typeface = Typeface.DEFAULT
    }

    // ========== HUD (Score, Combo, Multiplier) ==========

    private fun drawHUD(canvas: Canvas, state: GameState) {
        val padding = 20f

        // Score: top-right
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 42f
        textPaint.color = Color.WHITE
        val scoreX = screenWidth - padding
        val scoreY = screenHeight * 0.05f + 40f
        canvas.drawText("${state.score}", scoreX, scoreY, textPaint)

        smallTextPaint.textAlign = Paint.Align.RIGHT
        smallTextPaint.color = Color.argb(90, 255, 255, 255)
        smallTextPaint.textSize = 15f
        canvas.drawText("SCORE", scoreX, scoreY + 17f, smallTextPaint)

        // Multiplier below score
        if (state.comboMultiplier > 1.0f) {
            val multText = "${state.comboMultiplier.toInt()}x"
            textPaint.textAlign = Paint.Align.RIGHT
            textPaint.textSize = 34f
            textPaint.color = when {
                state.comboMultiplier >= 4f -> Color.rgb(255, 215, 0)
                state.comboMultiplier >= 3f -> Color.rgb(255, 152, 0)
                state.comboMultiplier >= 2f -> Color.rgb(0, 230, 118)
                else -> Color.WHITE
            }
            canvas.drawText(multText, scoreX, scoreY + 50f, textPaint)
        }

        // Combo / Note Streak: above strikeline, centered
        if (state.combo > 2) {
            val comboX = screenWidth / 2
            val comboY = highwayBottomY - fretButtonRadius * 2.8f

            val comboSize = when {
                state.combo >= 100 -> 46f
                state.combo >= 50 -> 40f
                state.combo >= 20 -> 34f
                else -> 28f
            }

            // Glow for high combos
            if (state.combo >= 20) {
                val glowColor = when {
                    state.combo >= 100 -> Color.rgb(255, 215, 0)
                    state.combo >= 50 -> Color.rgb(255, 87, 34)
                    else -> Color.rgb(156, 39, 176)
                }
                comboGlowPaint.color = glowColor
                comboGlowPaint.alpha = 90
                comboGlowPaint.textSize = comboSize + 4f
                canvas.drawText("${state.combo}", comboX, comboY, comboGlowPaint)
            }

            val comboColor = when {
                state.combo >= 100 -> Color.rgb(255, 215, 0)
                state.combo >= 50 -> Color.rgb(255, 152, 0)
                state.combo >= 20 -> Color.rgb(156, 39, 176)
                else -> Color.WHITE
            }
            comboPaint.color = comboColor
            comboPaint.textSize = comboSize
            canvas.drawText("${state.combo}", comboX, comboY, comboPaint)

            smallTextPaint.textAlign = Paint.Align.CENTER
            smallTextPaint.textSize = 13f
            smallTextPaint.color = Color.argb(120, 255, 255, 255)
            canvas.drawText("NOTE STREAK", comboX, comboY + 16f, smallTextPaint)
        }

        // Reset
        textPaint.textSize = 48f
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.color = Color.WHITE
        smallTextPaint.color = Color.WHITE
        smallTextPaint.alpha = 255
        smallTextPaint.textSize = 32f
    }

    // ========== PROGRESS BAR ==========

    private fun drawProgressBar(canvas: Canvas, state: GameState) {
        val barHeight = 5f
        canvas.drawRect(0f, 0f, screenWidth, barHeight, progressBgPaint)

        if (state.songDurationMs > 0) {
            val progress = (state.currentTimeMs.toFloat() / state.songDurationMs).coerceIn(0f, 1f)
            val fillWidth = screenWidth * progress

            progressFillPaint.shader = LinearGradient(
                0f, 0f, fillWidth, 0f,
                Color.rgb(156, 39, 176), Color.rgb(0, 188, 212),
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(0f, 0f, fillWidth, barHeight, progressFillPaint)
            progressFillPaint.shader = null
        }
    }

    // ========== COUNTDOWN ==========

    private fun drawCountdown(canvas: Canvas, value: Int) {
        val overlay = Paint().apply {
            color = Color.BLACK
            alpha = 170
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlay)

        val time = System.currentTimeMillis()
        val scale = 1f + (sin(time * 0.01).toFloat() + 1f) * 0.05f

        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 180f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            value.toString(),
            screenWidth / 2,
            screenHeight / 2 + 60f,
            countPaint
        )

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(156, 39, 176)
            textSize = 180f * scale
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maskFilter = BlurMaskFilter(30f, BlurMaskFilter.Blur.NORMAL)
            alpha = 120
        }
        canvas.drawText(
            value.toString(),
            screenWidth / 2,
            screenHeight / 2 + 60f,
            glowPaint
        )
    }

    // ========== PAUSE OVERLAY ==========

    private fun drawPauseOverlay(canvas: Canvas) {
        val overlay = Paint().apply {
            color = Color.BLACK
            alpha = 190
        }
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, overlay)

        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = 64f
        textPaint.color = Color.WHITE
        canvas.drawText("PAUSED", screenWidth / 2, screenHeight / 2 - 20f, textPaint)

        smallTextPaint.textAlign = Paint.Align.CENTER
        smallTextPaint.color = Color.argb(160, 255, 255, 255)
        smallTextPaint.textSize = 28f
        canvas.drawText("Tap to resume", screenWidth / 2, screenHeight / 2 + 30f, smallTextPaint)
        textPaint.textSize = 48f
        smallTextPaint.textSize = 32f
        smallTextPaint.color = Color.WHITE
    }
}
