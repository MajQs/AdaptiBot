package com.adaptibot.hotkey

import com.adaptibot.hotkey.domain.GlobalHotkeyService
import com.adaptibot.hotkey.model.HotkeyCombination

sealed interface HotkeyRegistrationResult {
    data object Registered : HotkeyRegistrationResult
    data class Unavailable(val reason: String) : HotkeyRegistrationResult

    companion object {
        const val PLATFORM_UNSUPPORTED_REASON =
            "Global hotkeys are unavailable on this platform."
    }
}

/**
 * The module is responsible for **system-wide keyboard shortcuts**.
 *
 * This module provides an emergency shortcut that works regardless of which window
 * currently has focus.
 *
 * The facade owns operating-system resources — [close] must be called on application shutdown,
 * otherwise the shortcut stays reserved for the whole process lifetime.
 *
 * @see HotkeyConfiguration
 */
class HotkeyFacade internal constructor(
    private val globalHotkeyService: GlobalHotkeyService,
    private val stopCombination: HotkeyCombination
) : AutoCloseable {

    /** Human readable shortcut, e.g. `Ctrl+Shift+F12` — meant to be shown in the UI. */
    val stopHotkeyName: String get() = stopCombination.displayName

    /**
     * Registers the emergency stop shortcut.
     *
     * @param onStopRequested invoked on a background thread every time the shortcut is pressed;
     *        dispatching to the UI thread is the caller's responsibility
     * @return `false` when the shortcut is already taken by another application — the application
     *         stays fully usable, only the shortcut is unavailable
     */
    fun registerStopHotkey(onStopRequested: () -> Unit): Boolean =
        registerStopHotkeyWithStatus(onStopRequested) == HotkeyRegistrationResult.Registered

    /** Registers the emergency shortcut and exposes a stable reason when registration is unavailable. */
    fun registerStopHotkeyWithStatus(onStopRequested: () -> Unit): HotkeyRegistrationResult =
        globalHotkeyService.register(stopCombination, onStopRequested)

    /** Releases every registered shortcut. Idempotent. */
    override fun close() = globalHotkeyService.close()
}
