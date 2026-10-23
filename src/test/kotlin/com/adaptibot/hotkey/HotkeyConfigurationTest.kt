package com.adaptibot.hotkey

import com.adaptibot.infrastructure.PlatformCapabilities
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class HotkeyConfigurationTest {

    @Test
    fun `unsupported hotkey registration reports a stable unavailable result`() {
        val facade = HotkeyConfiguration.getFacade(unsupportedCapabilities())

        val result = facade.registerStopHotkeyWithStatus {
            error("Unsupported hotkey must never invoke its callback")
        }

        assertEquals(
            HotkeyRegistrationResult.Unavailable(HotkeyRegistrationResult.PLATFORM_UNSUPPORTED_REASON),
            result
        )
        assertFalse(facade.registerStopHotkey {})
        assertDoesNotThrow {
            facade.close()
            facade.close()
        }
    }

    private fun unsupportedCapabilities() = PlatformCapabilities(
        editorSupported = true,
        actionSupported = false,
        globalHotkeySupported = false,
        recordingSupported = false,
        
    )
}
