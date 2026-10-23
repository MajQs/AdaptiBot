package com.adaptibot.execution

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.execution.domain.ExecutionEventPublisher
import com.adaptibot.execution.dto.ObserverStatusDto
import com.adaptibot.infrastructure.PlatformCapabilities
import com.adaptibot.script.Script
import com.adaptibot.script.step.ActionStep
import com.adaptibot.script.value.Action
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ExecutionConfigurationTest {

    @Test
    fun `unsupported execution configuration completes a system wait normally`() {
        val events = RecordingExecutionEventPublisher()
        val facade = ExecutionConfiguration.getFacade(events, unsupportedCapabilities())
        val script = scriptWith(Action.System.Wait(milliseconds = 0))

        try {
            facade.startScript(script)

            assertTrue(events.stepCompleted.await(2, TimeUnit.SECONDS))
            assertEquals(listOf("wait"), events.successes.take(1))
            assertTrue(events.failures.isEmpty())
        } finally {
            facade.stopScript()
        }
    }

    @Test
    fun `unsupported native action reaches execution failure logging`() {
        val events = RecordingExecutionEventPublisher()
        val facade = ExecutionConfiguration.getFacade(events, unsupportedCapabilities())
        val script = scriptWith(Action.Keyboard.TypeText("text"))

        try {
            facade.startScript(script)

            assertTrue(events.stepCompleted.await(2, TimeUnit.SECONDS))
            assertTrue(events.successes.isEmpty())
            assertEquals(
                listOf("keyboard" to ActionExecutionException.NATIVE_ACTIONS_UNSUPPORTED_MESSAGE),
                events.failures.take(1)
            )
        } finally {
            facade.stopScript()
        }
    }

    private fun scriptWith(action: Action): Script = Script.create("test").also { script ->
        script.addStep(
            containerId = script.rootContainerId,
            step = ActionStep(label = if (action is Action.System) "wait" else "keyboard", action = action)
        )
    }

    private fun unsupportedCapabilities() = PlatformCapabilities(
        editorSupported = true,
        actionSupported = false,
        globalHotkeySupported = false,
        recordingSupported = false,
        
    )

    private class RecordingExecutionEventPublisher : ExecutionEventPublisher {
        val successes = mutableListOf<String>()
        val failures = mutableListOf<Pair<String, String>>()
        val stepCompleted = CountDownLatch(1)

        override fun logExecutionStart(scriptName: String) = Unit

        override fun logExecutionStop() = Unit

        override fun logStepSuccess(stepName: String, durationMs: Long) {
            successes += stepName
            stepCompleted.countDown()
        }

        override fun logStepFailure(stepName: String, durationMs: Long, error: String) {
            failures += stepName to error
            stepCompleted.countDown()
        }

        override fun observerStatusChanged(status: ObserverStatusDto) = Unit
    }
}
