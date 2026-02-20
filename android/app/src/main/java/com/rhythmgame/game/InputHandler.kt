package com.rhythmgame.game

import android.view.MotionEvent

class InputHandler(
    private val laneCount: Int = 5,
    private val screenWidth: Float,
    private val highwayBottomLeft: Float = 0f,
    private val highwayBottomWidth: Float = screenWidth,
) {
    data class TouchEvent(
        val lane: Int,
        val action: TouchAction,
    )

    enum class TouchAction {
        DOWN, UP
    }

    private val laneWidth: Float get() = highwayBottomWidth / laneCount

    fun processMotionEvent(event: MotionEvent): List<TouchEvent> {
        val events = mutableListOf<TouchEvent>()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val lane = getLaneFromX(x)
                if (lane in 1..laneCount) {
                    events.add(TouchEvent(lane, TouchAction.DOWN))
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val lane = getLaneFromX(x)
                if (lane in 1..laneCount) {
                    events.add(TouchEvent(lane, TouchAction.UP))
                }
            }
        }

        return events
    }

    fun getLaneFromX(x: Float): Int {
        val relativeX = x - highwayBottomLeft
        if (relativeX < 0 || relativeX > highwayBottomWidth) return -1
        val lane = (relativeX / laneWidth).toInt() + 1
        return lane.coerceIn(1, laneCount)
    }
}
