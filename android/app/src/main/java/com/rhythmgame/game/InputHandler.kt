package com.rhythmgame.game

import android.view.MotionEvent

class InputHandler(
    private val laneCount: Int = 5,
    private val screenWidth: Float,
    private val highwayBottomLeft: Float = 0f,
    private val highwayBottomWidth: Float = screenWidth,
    private val arcRenderer: ArcGameRenderer? = null,
) {
    data class TouchEvent(
        val lane: Int,
        val action: TouchAction,
        val pointerId: Int = 0,
    )

    enum class TouchAction {
        DOWN, UP
    }

    // Track which lane each pointer is currently in (for glissando)
    private val pointerLaneMap = mutableMapOf<Int, Int>()
    // Pointers to ignore (e.g., touching power bar zone)
    private val ignoredPointers = mutableSetOf<Int>()

    private val laneWidth: Float get() = highwayBottomWidth / laneCount

    /**
     * Mark a pointer as ignored so it won't generate lane events.
     * Used for pointers that touch non-lane zones (power bar, etc.)
     */
    fun ignorePointer(pointerId: Int) {
        ignoredPointers.add(pointerId)
        pointerLaneMap.remove(pointerId)
    }

    /**
     * Clear all pointer tracking state. Call on pause/surface destroyed.
     */
    fun reset() {
        pointerLaneMap.clear()
        ignoredPointers.clear()
    }

    fun processMotionEvent(event: MotionEvent): List<TouchEvent> {
        val events = mutableListOf<TouchEvent>()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId in ignoredPointers) return events

                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                val lane = getLaneFromXY(x, y)
                if (lane in 1..laneCount) {
                    pointerLaneMap[pointerId] = lane
                    events.add(TouchEvent(lane, TouchAction.DOWN, pointerId))
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Check ALL active pointers for lane transitions (glissando)
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    if (pointerId in ignoredPointers) continue

                    val x = event.getX(i)
                    val y = event.getY(i)
                    val newLane = getLaneFromXY(x, y)
                    val currentLane = pointerLaneMap[pointerId]

                    if (currentLane != null && newLane != currentLane) {
                        // Finger slid to a different lane — emit UP for old, DOWN for new
                        events.add(TouchEvent(currentLane, TouchAction.UP, pointerId))

                        if (newLane in 1..laneCount) {
                            pointerLaneMap[pointerId] = newLane
                            events.add(TouchEvent(newLane, TouchAction.DOWN, pointerId))
                        } else {
                            // Slid off the highway entirely
                            pointerLaneMap.remove(pointerId)
                        }
                    } else if (currentLane == null && newLane in 1..laneCount) {
                        // Pointer entered highway from outside
                        pointerLaneMap[pointerId] = newLane
                        events.add(TouchEvent(newLane, TouchAction.DOWN, pointerId))
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                // Last pointer lifted — release it and clear all tracking
                val pointerId = event.getPointerId(event.actionIndex)
                ignoredPointers.remove(pointerId)
                val lane = pointerLaneMap.remove(pointerId)
                if (lane != null) {
                    events.add(TouchEvent(lane, TouchAction.UP, pointerId))
                }
                // Safety: release any remaining tracked pointers
                for ((id, l) in pointerLaneMap) {
                    events.add(TouchEvent(l, TouchAction.UP, id))
                }
                pointerLaneMap.clear()
                ignoredPointers.clear()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                if (pointerId in ignoredPointers) {
                    ignoredPointers.remove(pointerId)
                    return events
                }
                val lane = pointerLaneMap.remove(pointerId)
                if (lane != null) {
                    events.add(TouchEvent(lane, TouchAction.UP, pointerId))
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                // Parent intercepted — release all tracked pointers
                for ((id, lane) in pointerLaneMap) {
                    events.add(TouchEvent(lane, TouchAction.UP, id))
                }
                pointerLaneMap.clear()
                ignoredPointers.clear()
            }
        }

        return events
    }

    fun getLaneFromXY(x: Float, y: Float = 0f): Int {
        if (arcRenderer != null) {
            return arcRenderer.getLaneFromTouch(x, y)
        }
        val relativeX = x - highwayBottomLeft
        if (relativeX < 0 || relativeX > highwayBottomWidth) return -1
        val lane = (relativeX / laneWidth).toInt() + 1
        return lane.coerceIn(1, laneCount)
    }
}
