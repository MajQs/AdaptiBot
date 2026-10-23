package com.adaptibot.recorder

import com.adaptibot.infrastructure.PlatformCapabilities
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class RecorderConfigurationTest {

    @Test
    fun `unsupported recorder cannot start and has idempotent lifecycle`() {
        val facade = RecorderConfiguration.getFacade(unsupportedCapabilities())

        assertFalse(facade.start { error("Unsupported recording must not complete a session") })
        assertFalse(facade.isRecording())
        assertDoesNotThrow {
            facade.stop()
            facade.stop()
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
