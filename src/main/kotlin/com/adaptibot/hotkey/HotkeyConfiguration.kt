package com.adaptibot.hotkey

import com.adaptibot.hotkey.adapter.unsupported.UnsupportedGlobalHotkeyService
import com.adaptibot.hotkey.adapter.winapi.WinApiHotkeyNativeApi
import com.adaptibot.hotkey.adapter.winapi.WindowsHotkeyService
import com.adaptibot.hotkey.domain.GlobalHotkeyService
import com.adaptibot.hotkey.model.HotkeyCombination
import com.adaptibot.infrastructure.PlatformCapabilities
import com.adaptibot.infrastructure.PlatformCapabilityDetector

object HotkeyConfiguration {

    internal fun getFacade(capabilities: PlatformCapabilities): HotkeyFacade = create(
        globalHotkeyService = if (capabilities.globalHotkeySupported) {
            WindowsHotkeyService(WinApiHotkeyNativeApi())
        } else {
            UnsupportedGlobalHotkeyService()
        }
    )

    internal fun create(globalHotkeyService: GlobalHotkeyService): HotkeyFacade = HotkeyFacade(
        globalHotkeyService = globalHotkeyService,
        stopCombination = HotkeyCombination.STOP_EXECUTION
    )
}
