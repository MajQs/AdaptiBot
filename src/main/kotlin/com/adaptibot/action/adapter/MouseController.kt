package com.adaptibot.action.adapter

import com.adaptibot.action.adapter.winapi.User32
import com.adaptibot.script.MouseClickType
import com.adaptibot.script.Coordinate
import com.adaptibot.script.MouseButton
import com.adaptibot.script.MouseScrollDirection
import org.slf4j.LoggerFactory

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
            Thread.sleep(100)
            performSingleClick(button, holdDuration)
        }
        MouseClickType.TRIPLE -> {
            performSingleClick(button, holdDuration)
            Thread.sleep(100)
            performSingleClick(button, holdDuration)
            Thread.sleep(100)
            performSingleClick(button, holdDuration)
        }
    }

    private fun performSingleClick(button: MouseButton, holdDuration: Long) {
        val (downFlag, upFlag) = when (button) {
            MouseButton.LEFT   -> Pair(User32.MOUSEEVENTF_LEFTDOWN,   User32.MOUSEEVENTF_LEFTUP)
            MouseButton.RIGHT  -> Pair(User32.MOUSEEVENTF_RIGHTDOWN,  User32.MOUSEEVENTF_RIGHTUP)
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
