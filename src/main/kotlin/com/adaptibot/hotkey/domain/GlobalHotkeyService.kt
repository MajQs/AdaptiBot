package com.adaptibot.hotkey.domain

import com.adaptibot.hotkey.model.HotkeyCombination

internal interface GlobalHotkeyService : AutoCloseable {

    fun register(combination: HotkeyCombination, onTriggered: () -> Unit): Boolean

    fun unregister(combination: HotkeyCombination)

    override fun close()
}

