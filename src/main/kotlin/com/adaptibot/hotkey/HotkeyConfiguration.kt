package com.adaptibot.hotkey

import com.adaptibot.hotkey.adapter.winapi.WinApiHotkeyNativeApi
import com.adaptibot.hotkey.adapter.winapi.WindowsHotkeyService
import com.adaptibot.hotkey.domain.HotkeyNativeApi
import com.adaptibot.hotkey.model.HotkeyCombination

object HotkeyConfiguration {

    fun getFacade(): HotkeyFacade = getFacade(WinApiHotkeyNativeApi())

    internal fun getFacade(
        nativeApi: HotkeyNativeApi,
        stopCombination: HotkeyCombination = HotkeyCombination.STOP_EXECUTION
    ): HotkeyFacade = HotkeyFacade(
        globalHotkeyService = WindowsHotkeyService(nativeApi),
        stopCombination = stopCombination
    )
}

