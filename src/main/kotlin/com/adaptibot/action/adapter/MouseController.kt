package com.adaptibot.action.adapter

import com.adaptibot.action.adapter.winapi.User32
import com.adaptibot.infrastructure.InterruptibleSleep
import com.adaptibot.script.value.MouseClickType
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.MouseButton
import com.adaptibot.script.value.MouseScrollDirection
import org.slf4j.LoggerFactory
import kotlin.math.pow

internal object MouseController {

    private val logger = LoggerFactory.getLogger(MouseController::class.java)
    private val user32 = User32.INSTANCE


    private fun moveTo(x: Int, y: Int) {
        try {
            user32.SetCursorPos(x, y)
        } catch (e: Exception) {
            logger.error("Failed to move cursor to ($x, $y)", e)
        }
    }

    fun moveTo(coordinate: Coordinate) {
        val physical = DpiScaler.toPhysical(coordinate)
        moveTo(physical.x, physical.y)
    }

    fun click(
        button: MouseButton = MouseButton.LEFT,
        type: MouseClickType = MouseClickType.SINGLE,
        holdDuration: Long = 0L
    ) = when (type) {
        MouseClickType.SINGLE -> performSingleClick(button, holdDuration)
        MouseClickType.DOUBLE -> {
            performSingleClick(button, holdDuration)
            InterruptibleSleep.sleep(100)
            performSingleClick(button, holdDuration)
        }
        MouseClickType.TRIPLE -> {
            performSingleClick(button, holdDuration)
            InterruptibleSleep.sleep(100)
            performSingleClick(button, holdDuration)
            InterruptibleSleep.sleep(100)
            performSingleClick(button, holdDuration)
        }
    }

    private fun performSingleClick(button: MouseButton, holdDuration: Long) {
        val (downFlag, upFlag) = when (button) {
            MouseButton.LEFT   -> Pair(User32.MOUSEEVENTF_LEFTDOWN,   User32.MOUSEEVENTF_LEFTUP)
            MouseButton.RIGHT  -> Pair(User32.MOUSEEVENTF_RIGHTDOWN,  User32.MOUSEEVENTF_RIGHTUP)
            MouseButton.MIDDLE -> Pair(User32.MOUSEEVENTF_MIDDLEDOWN, User32.MOUSEEVENTF_MIDDLEUP)
        }
        InputStateTracker.holdingMouseButton(downFlag, upFlag) {
            InterruptibleSleep.sleep(holdDuration)
        }
    }

    fun drag(from: Coordinate?, to: Coordinate, durationMs: Long = 500, fps: Int = 60) {
        val fromCor = from ?: run {
            val point = User32.POINT()
             user32.GetCursorPos(point)
            Coordinate(point.x, point.y)
        }

        moveTo(fromCor)

        val steps = (durationMs * fps / 1000).toInt().coerceAtLeast(1)
        val stepDelay = durationMs / steps

        InputStateTracker.holdingMouseButton(User32.MOUSEEVENTF_LEFTDOWN, User32.MOUSEEVENTF_LEFTUP) {
            for (i in 0..steps) {
                val t = i.toFloat() / steps
                // Ease-InOut-Cubic
                val easedT = if (t < 0.5f) {
                    4 * t * t * t
                } else {
                    1 - (-2 * t + 2).toDouble().pow(3.0).toFloat() / 2
                }

                val currentX = (fromCor.x + (to.x - fromCor.x) * easedT).toInt()
                val currentY = (fromCor.y + (to.y - fromCor.y) * easedT).toInt()

                moveTo(Coordinate(currentX, currentY))
                InterruptibleSleep.sleep(stepDelay)
            }
        }
    }

    fun scroll(amount: Int, direction: MouseScrollDirection) {
        val wheelDelta = when (direction) {
            MouseScrollDirection.UP -> User32.Companion.WHEEL_DELTA * amount
            MouseScrollDirection.DOWN -> -User32.Companion.WHEEL_DELTA * amount
            MouseScrollDirection.LEFT, MouseScrollDirection.RIGHT -> {
                logger.warn("Horizontal scrolling not yet implemented")
            }
        }
    }
}
