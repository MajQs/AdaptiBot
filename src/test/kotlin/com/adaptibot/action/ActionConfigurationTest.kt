package com.adaptibot.action

import com.adaptibot.infrastructure.PlatformCapabilities
import com.adaptibot.script.value.Action
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActionConfigurationTest {

    @Test
    fun `unsupported configuration keeps system actions available`() {
        val facade = ActionConfiguration.getActionFacade(unsupportedCapabilities())

        assertDoesNotThrow { facade.execute(Action.System.Wait(milliseconds = 0)) }
    }

    @Test
    fun `unsupported configuration rejects mouse and keyboard actions explicitly`() {
        val facade = ActionConfiguration.getActionFacade(unsupportedCapabilities())

        listOf(
            Action.Mouse.Click(),
            Action.Keyboard.TypeText("text")
        ).forEach { action ->
            val failure = assertThrows(ActionExecutionException::class.java) {
                facade.execute(action)
            }

            assertInstanceOf(ActionExecutionException.UnsupportedPlatform::class.java, failure)
            assertEquals(ActionExecutionException.NATIVE_ACTIONS_UNSUPPORTED_MESSAGE, failure.message)
        }
    }

    private fun unsupportedCapabilities() = PlatformCapabilities(
        editorSupported = true,
        actionSupported = false,
        globalHotkeySupported = false,
        recordingSupported = false,
        
    )
}
