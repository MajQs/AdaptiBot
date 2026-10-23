package com.adaptibot.action.domain

import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.KeyboardKey
import com.adaptibot.script.value.MouseButton
import com.adaptibot.script.value.MouseClickType
import com.adaptibot.script.value.MouseScrollDirection

/** Outbound port for physical mouse and keyboard automation. */
internal interface InputController {
    fun moveMouseTo(coordinate: Coordinate)

    fun clickMouse(button: MouseButton, type: MouseClickType, holdDuration: Long)

    fun dragMouse(from: Coordinate?, to: Coordinate)

    fun scrollMouse(amount: Int, direction: MouseScrollDirection)

    fun typeText(text: String)

    fun pressKeys(keys: List<KeyboardKey>)

    fun releaseAllInputs()
}
