package com.adaptibot.ui.viewmodel

import com.adaptibot.execution.ExecutionFacade
import io.mockk.mockk
import com.adaptibot.infrastructure.PlatformCapabilities
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScriptViewModelCapabilityTest {

    private val windows = PlatformCapabilities(
        editorSupported = true,
        actionSupported = true,
        globalHotkeySupported = true,
        recordingSupported = true,
        
    )
    private val editorOnly = PlatformCapabilities(
        editorSupported = true,
        actionSupported = false,
        globalHotkeySupported = false,
        recordingSupported = false,
        
    )

    private fun viewModel(capabilities: PlatformCapabilities) = ScriptViewModel(mockk<ExecutionFacade>(relaxed = true), capabilities)

    @Test
    fun `supported platform exposes every capability as available`() {
        val vm = viewModel(windows)
        assertTrue(vm.runAvailableProperty.get())
        assertTrue(vm.recordingAvailableProperty.get())
        assertTrue(vm.globalHotkeyAvailableProperty.get())
        assertTrue(vm.nativeStepCreationAvailableProperty.get())
    }

    @Test
    fun `unsupported platform constructs and exposes every capability as unavailable`() {
        val vm = viewModel(editorOnly)
        assertFalse(vm.runAvailableProperty.get())
        assertFalse(vm.recordingAvailableProperty.get())
        assertFalse(vm.globalHotkeyAvailableProperty.get())
        assertFalse(vm.nativeStepCreationAvailableProperty.get())
    }
}
