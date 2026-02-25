package com.adaptibot.action.adapter

import com.adaptibot.action.adapter.winapi.User32
import com.adaptibot.common.model.ClickType
import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.MouseButton
import com.adaptibot.common.model.ScrollDirection
import org.slf4j.LoggerFactory

internal object MouseController {

    private val logger = LoggerFactory.getLogger(MouseController::class.java)
    private val user32 = User32.Companion.INSTANCE

    private fun moveTo(x: Int, y: Int) {
        try {
            user32.SetCursorPos(x, y)
        } catch (e: Exception) {
            logger.error("Failed to move cursor to ($x, $y)", e)
        }
    }

    fun moveTo(coordinate: Coordinate) = moveTo(coordinate.x, coordinate.y)

    fun click(
        button: MouseButton = MouseButton.LEFT,
        type: ClickType = ClickType.SINGLE,
        holdDuration: Long = 0L
    ) = when (type) {
        ClickType.SINGLE -> performSingleClick(button, holdDuration)
        ClickType.DOUBLE -> {
            performSingleClick(button, holdDuration)
            Thread.sleep(100)
            performSingleClick(button, holdDuration)
        }

        ClickType.TRIPLE -> {
            performSingleClick(button, holdDuration)
            Thread.sleep(100)
            performSingleClick(button, holdDuration)
            Thread.sleep(100)
            performSingleClick(button, holdDuration)
        }
    }

    private fun performSingleClick(button: MouseButton, holdDuration: Long) {
        val (downFlag, upFlag) = when (button) {
            MouseButton.LEFT -> Pair(User32.MOUSEEVENTF_LEFTDOWN, User32.MOUSEEVENTF_LEFTUP)
            MouseButton.RIGHT -> Pair(User32.MOUSEEVENTF_RIGHTDOWN, User32.MOUSEEVENTF_RIGHTUP)
            MouseButton.MIDDLE -> Pair(User32.MOUSEEVENTF_MIDDLEDOWN, User32.MOUSEEVENTF_MIDDLEUP)
        }
        user32.mouse_event(downFlag, 0, 0, 0, 0)
        Thread.sleep(holdDuration)
        user32.mouse_event(upFlag, 0, 0, 0, 0)
    }

    fun drag(from: Coordinate?, to: Coordinate) {
        if (from != null) {
            moveTo(from)
        }
        user32.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
        Thread.sleep(50)
        moveTo(to)     // TODO implement smooth dragging with configurable speed
        Thread.sleep(50)
        user32.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
    }

    fun scroll(amount: Int, direction: ScrollDirection) {
        val wheelDelta = when (direction) {
            ScrollDirection.UP -> User32.Companion.WHEEL_DELTA * amount
            ScrollDirection.DOWN -> -User32.Companion.WHEEL_DELTA * amount
            ScrollDirection.LEFT, ScrollDirection.RIGHT -> {
                logger.warn("Horizontal scrolling not yet implemented")

            }
        }
        user32.mouse_event(User32.Companion.MOUSEEVENTF_WHEEL, 0, 0, wheelDelta as Int, 0)
    }
}


