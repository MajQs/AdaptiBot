package com.adaptibot.action.adapter.robot

import com.adaptibot.action.domain.InputController
import com.adaptibot.infrastructure.InterruptibleSleep
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.KeyboardKey
import com.adaptibot.script.value.MouseButton
import com.adaptibot.script.value.MouseClickType
import com.adaptibot.script.value.MouseScrollDirection
import java.awt.AWTException
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow

internal class RobotInputController(
    private val robot: Robot = createRobot()
) : InputController {

    private val heldKeys = ConcurrentHashMap.newKeySet<Int>()
    private val heldMouseButtons = ConcurrentHashMap.newKeySet<Int>()

    init {
        robot.autoDelay = 0
        robot.isAutoWaitForIdle = false
    }

    override fun moveMouseTo(coordinate: Coordinate) = robot.mouseMove(coordinate.x, coordinate.y)

    override fun clickMouse(button: MouseButton, type: MouseClickType, holdDuration: Long) {
        repeat(
            when (type) {
                MouseClickType.SINGLE -> 1
                MouseClickType.DOUBLE -> 2
                MouseClickType.TRIPLE -> 3
            }
        ) { clickOnce(button, holdDuration) }
    }

    override fun dragMouse(from: Coordinate?, to: Coordinate) {
        val start = from ?: java.awt.MouseInfo.getPointerInfo().location.let { Coordinate(it.x, it.y) }
        moveMouseTo(start)
        val durationMs = 500L
        val steps = (durationMs * 60 / 1000).toInt().coerceAtLeast(1)
        val stepDelay = durationMs / steps
        holdMouseButton(MouseButton.LEFT) {
            for (index in 0..steps) {
                val t = index.toFloat() / steps
                val easedT = if (t < 0.5f) {
                    4 * t * t * t
                } else {
                    1 - (-2 * t + 2).toDouble().pow(3.0).toFloat() / 2
                }
                moveMouseTo(
                    Coordinate(
                        (start.x + (to.x - start.x) * easedT).toInt(),
                        (start.y + (to.y - start.y) * easedT).toInt()
                    )
                )
                InterruptibleSleep.sleep(stepDelay)
            }
        }
    }

    override fun scrollMouse(amount: Int, direction: MouseScrollDirection) {
        val signedAmount = when (direction) {
            MouseScrollDirection.UP -> -amount
            MouseScrollDirection.DOWN -> amount
            MouseScrollDirection.LEFT, MouseScrollDirection.RIGHT ->
                throw UnsupportedOperationException("Horizontal scrolling is not supported by Robot")
        }
        robot.mouseWheel(signedAmount)
    }

    override fun typeText(text: String) {
        text.forEach { character ->
            typeCharacter(character)
            InterruptibleSleep.sleep(TYPE_CHARACTER_GAP_MS)
        }
    }

    override fun pressKeys(keys: List<KeyboardKey>) {
        val keyCodes = keys.map(::keyCode)
        keyCodes.forEach { pressKey(it) }
        try {
            InterruptibleSleep.sleep(COMBINATION_HOLD_MS)
        } finally {
            keyCodes.asReversed().forEach(::releaseKey)
        }
    }

    override fun releaseAllInputs() {
        heldMouseButtons.toList().forEach { buttonMask ->
            runCatching { robot.mouseRelease(buttonMask) }
            heldMouseButtons.remove(buttonMask)
        }
        heldKeys.toList().forEach { keyCode ->
            runCatching { robot.keyRelease(keyCode) }
            heldKeys.remove(keyCode)
        }
    }

    private fun clickOnce(button: MouseButton, holdDuration: Long) {
        holdMouseButton(button) { InterruptibleSleep.sleep(holdDuration) }
    }

    private fun holdMouseButton(button: MouseButton, whileHeld: () -> Unit) {
        val mask = mouseMask(button)
        heldMouseButtons.add(mask)
        robot.mousePress(mask)
        try {
            whileHeld()
        } finally {
            robot.mouseRelease(mask)
            heldMouseButtons.remove(mask)
        }
    }

    private fun typeCharacter(character: Char) {
        val keyCode = KeyEvent.getExtendedKeyCodeForChar(character.code)
        require(keyCode != KeyEvent.VK_UNDEFINED) { "Unsupported character: '$character'" }
        pressKey(keyCode)
        try {
            InterruptibleSleep.sleep(TYPE_CHARACTER_HOLD_MS)
        } finally {
            releaseKey(keyCode)
        }
    }

    private fun pressKey(keyCode: Int) {
        heldKeys.add(keyCode)
        robot.keyPress(keyCode)
        InterruptibleSleep.sleep(KEY_PRESS_GAP_MS)
    }

    private fun releaseKey(keyCode: Int) {
        runCatching { robot.keyRelease(keyCode) }
        heldKeys.remove(keyCode)
    }

    private fun mouseMask(button: MouseButton): Int = when (button) {
        MouseButton.LEFT -> InputEvent.BUTTON1_DOWN_MASK
        MouseButton.RIGHT -> InputEvent.BUTTON3_DOWN_MASK
        MouseButton.MIDDLE -> InputEvent.BUTTON2_DOWN_MASK
    }

    private fun keyCode(key: KeyboardKey): Int = when (key) {
        KeyboardKey.A -> KeyEvent.VK_A
        KeyboardKey.B -> KeyEvent.VK_B
        KeyboardKey.C -> KeyEvent.VK_C
        KeyboardKey.D -> KeyEvent.VK_D
        KeyboardKey.E -> KeyEvent.VK_E
        KeyboardKey.F -> KeyEvent.VK_F
        KeyboardKey.G -> KeyEvent.VK_G
        KeyboardKey.H -> KeyEvent.VK_H
        KeyboardKey.I -> KeyEvent.VK_I
        KeyboardKey.J -> KeyEvent.VK_J
        KeyboardKey.K -> KeyEvent.VK_K
        KeyboardKey.L -> KeyEvent.VK_L
        KeyboardKey.M -> KeyEvent.VK_M
        KeyboardKey.N -> KeyEvent.VK_N
        KeyboardKey.O -> KeyEvent.VK_O
        KeyboardKey.P -> KeyEvent.VK_P
        KeyboardKey.Q -> KeyEvent.VK_Q
        KeyboardKey.R -> KeyEvent.VK_R
        KeyboardKey.S -> KeyEvent.VK_S
        KeyboardKey.T -> KeyEvent.VK_T
        KeyboardKey.U -> KeyEvent.VK_U
        KeyboardKey.V -> KeyEvent.VK_V
        KeyboardKey.W -> KeyEvent.VK_W
        KeyboardKey.X -> KeyEvent.VK_X
        KeyboardKey.Y -> KeyEvent.VK_Y
        KeyboardKey.Z -> KeyEvent.VK_Z
        KeyboardKey.DIGIT_0 -> KeyEvent.VK_0
        KeyboardKey.DIGIT_1 -> KeyEvent.VK_1
        KeyboardKey.DIGIT_2 -> KeyEvent.VK_2
        KeyboardKey.DIGIT_3 -> KeyEvent.VK_3
        KeyboardKey.DIGIT_4 -> KeyEvent.VK_4
        KeyboardKey.DIGIT_5 -> KeyEvent.VK_5
        KeyboardKey.DIGIT_6 -> KeyEvent.VK_6
        KeyboardKey.DIGIT_7 -> KeyEvent.VK_7
        KeyboardKey.DIGIT_8 -> KeyEvent.VK_8
        KeyboardKey.DIGIT_9 -> KeyEvent.VK_9
        KeyboardKey.F1 -> KeyEvent.VK_F1
        KeyboardKey.F2 -> KeyEvent.VK_F2
        KeyboardKey.F3 -> KeyEvent.VK_F3
        KeyboardKey.F4 -> KeyEvent.VK_F4
        KeyboardKey.F5 -> KeyEvent.VK_F5
        KeyboardKey.F6 -> KeyEvent.VK_F6
        KeyboardKey.F7 -> KeyEvent.VK_F7
        KeyboardKey.F8 -> KeyEvent.VK_F8
        KeyboardKey.F9 -> KeyEvent.VK_F9
        KeyboardKey.F10 -> KeyEvent.VK_F10
        KeyboardKey.F11 -> KeyEvent.VK_F11
        KeyboardKey.F12 -> KeyEvent.VK_F12
        KeyboardKey.CTRL -> KeyEvent.VK_CONTROL
        KeyboardKey.SHIFT -> KeyEvent.VK_SHIFT
        KeyboardKey.ALT -> KeyEvent.VK_ALT
        KeyboardKey.WIN -> KeyEvent.VK_META
        KeyboardKey.ENTER -> KeyEvent.VK_ENTER
        KeyboardKey.ESCAPE -> KeyEvent.VK_ESCAPE
        KeyboardKey.TAB -> KeyEvent.VK_TAB
        KeyboardKey.SPACE -> KeyEvent.VK_SPACE
        KeyboardKey.BACKSPACE -> KeyEvent.VK_BACK_SPACE
        KeyboardKey.CAPS_LOCK -> KeyEvent.VK_CAPS_LOCK
        KeyboardKey.PAUSE -> KeyEvent.VK_PAUSE
        KeyboardKey.HOME -> KeyEvent.VK_HOME
        KeyboardKey.END -> KeyEvent.VK_END
        KeyboardKey.PAGE_UP -> KeyEvent.VK_PAGE_UP
        KeyboardKey.PAGE_DOWN -> KeyEvent.VK_PAGE_DOWN
        KeyboardKey.ARROW_LEFT -> KeyEvent.VK_LEFT
        KeyboardKey.ARROW_UP -> KeyEvent.VK_UP
        KeyboardKey.ARROW_RIGHT -> KeyEvent.VK_RIGHT
        KeyboardKey.ARROW_DOWN -> KeyEvent.VK_DOWN
        KeyboardKey.INSERT -> KeyEvent.VK_INSERT
        KeyboardKey.DELETE -> KeyEvent.VK_DELETE
    }

    private companion object {
        const val TYPE_CHARACTER_HOLD_MS = 10L
        const val TYPE_CHARACTER_GAP_MS = 20L
        const val KEY_PRESS_GAP_MS = 20L
        const val COMBINATION_HOLD_MS = 50L

        fun createRobot(): Robot = try {
            Robot()
        } catch (failure: AWTException) {
            throw IllegalStateException("Could not initialize desktop input automation", failure)
        }
    }
}
