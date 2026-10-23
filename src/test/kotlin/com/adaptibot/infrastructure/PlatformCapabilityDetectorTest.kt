package com.adaptibot.infrastructure

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlatformCapabilityDetectorTest {

    @AfterEach
    fun clearPlatformOverride() {
        System.clearProperty(PlatformCapabilityDetector.PLATFORM_OVERRIDE_PROPERTY)
    }

    @Test
    fun `windows override enables every current capability`() {
        System.setProperty(PlatformCapabilityDetector.PLATFORM_OVERRIDE_PROPERTY, "windows")

        val capabilities = PlatformCapabilityDetector.detect()

        assertTrue(capabilities.editorSupported)
        assertTrue(capabilities.actionSupported)
        assertTrue(capabilities.globalHotkeySupported)
        assertTrue(capabilities.recordingSupported)
    }

    @Test
    fun `unsupported override selects editor-only capabilities`() {
        System.setProperty(PlatformCapabilityDetector.PLATFORM_OVERRIDE_PROPERTY, "unsupported")

        val capabilities = PlatformCapabilityDetector.detect()

        assertTrue(capabilities.editorSupported)
        assertFalse(capabilities.actionSupported)
        assertFalse(capabilities.globalHotkeySupported)
        assertFalse(capabilities.recordingSupported)
    }

    @Test
    fun `invalid override fails with accepted values in message`() {
        System.setProperty(PlatformCapabilityDetector.PLATFORM_OVERRIDE_PROPERTY, "solaris")

        val exception = assertThrows(IllegalArgumentException::class.java) {
            PlatformCapabilityDetector.detect()
        }

        assertTrue(exception.message.orEmpty().contains("adaptibot.platform"))
        assertTrue(exception.message.orEmpty().contains("windows, macos, linux, unsupported"))
    }
}
