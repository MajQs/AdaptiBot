package com.adaptibot.hotkey

import com.adaptibot.hotkey.adapter.winapi.WinApiHotkeyNativeApi
import com.adaptibot.hotkey.adapter.winapi.WindowsHotkeyService
import com.adaptibot.hotkey.model.HotkeyCombination

object HotkeyConfiguration {

    fun getFacade(): HotkeyFacade = HotkeyFacade(
        globalHotkeyService = WindowsHotkeyService(WinApiHotkeyNativeApi()),
        stopCombination = HotkeyCombination.STOP_EXECUTION
    )
}

