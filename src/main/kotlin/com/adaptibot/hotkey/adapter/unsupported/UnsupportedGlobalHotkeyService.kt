package com.adaptibot.hotkey.adapter.unsupported

import com.adaptibot.hotkey.HotkeyRegistrationResult
import com.adaptibot.hotkey.domain.GlobalHotkeyService
import com.adaptibot.hotkey.model.HotkeyCombination

internal class UnsupportedGlobalHotkeyService : GlobalHotkeyService {

    override fun register(
        combination: HotkeyCombination,
        onTriggered: () -> Unit
    ): HotkeyRegistrationResult = HotkeyRegistrationResult.Unavailable(
        HotkeyRegistrationResult.PLATFORM_UNSUPPORTED_REASON
    )

    override fun unregister(combination: HotkeyCombination) = Unit

    override fun close() = Unit
}
