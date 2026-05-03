package com.rhythmgame.game

/**
 * Shared geometry calculations for the guitar power bar.
 * Used by GameEngine (for touch detection) and both renderers (for drawing).
 */
class PowerBarGeometry(screenWidth: Float, screenHeight: Float, isArc: Boolean = false) {

    val guitarHeight: Float
    val guitarWidth: Float
    val guitarLeft: Float
    val guitarRight: Float
    val guitarTop: Float
    val guitarBottom: Float
    val guitarCenterX: Float

    init {
        if (isArc) {
            // Arc mode: smaller guitar in top-left corner, clear of the fan
            guitarHeight = screenHeight * 0.30f
            guitarWidth = guitarHeight * 0.667f
            guitarLeft = 6f
            guitarRight = guitarLeft + guitarWidth
            guitarTop = screenHeight * 0.03f
            guitarBottom = guitarTop + guitarHeight
        } else {
            // Highway mode: guitar to the right of the highway
            val hwBottomWidth = screenWidth * 0.58f
            val hwBottomRight = (screenWidth - hwBottomWidth) / 2f + hwBottomWidth
            guitarHeight = screenHeight * 0.88f
            guitarWidth = guitarHeight * 0.667f
            guitarLeft = hwBottomRight + 4f
            guitarRight = guitarLeft + guitarWidth
            guitarTop = (screenHeight - guitarHeight) / 2f
            guitarBottom = guitarTop + guitarHeight
        }
        guitarCenterX = (guitarLeft + guitarRight) / 2f
    }

    fun isInZone(x: Float, y: Float): Boolean {
        return x > guitarLeft && x < guitarRight && y > guitarTop && y < guitarBottom
    }
}
