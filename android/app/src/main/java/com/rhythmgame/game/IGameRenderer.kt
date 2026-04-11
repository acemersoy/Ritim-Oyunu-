package com.rhythmgame.game

import android.graphics.Canvas

interface IGameRenderer {
    fun render(canvas: Canvas, state: GameState)
    fun getHitLineY(): Float
    fun getHighwayBottomLeft(): Float
    fun getHighwayBottomWidth(): Float
    fun getStrikeLineY(): Float
    fun getLaneCenterXAtStrikeline(laneIndex: Int): Float
    fun getLaneColor(laneIndex: Int): Int
    fun getFretPosition(laneIndex: Int): Pair<Float, Float>
    fun getParticleDirection(laneIndex: Int): Pair<Float, Float>
}
