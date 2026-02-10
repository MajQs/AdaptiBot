package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState
import com.adaptibot.core.dto.ExecutionStateDto
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

internal class ScriptOrchestrator(
    private val stepExecutionOrchestrator: StepExecutionOrchestrator
) {
    private val logger = LoggerFactory.getLogger(ScriptOrchestrator::class.java)

    private var executionScope: CoroutineScope? = null
    private var currentContext: ExecutionContext = ExecutionContext(
        script = Script("", steps = emptyList()),
        state = ExecutionState.IDLE
    )

    @Volatile
    private var shouldStop = false

    fun start(script: Script) {
        if (currentContext.state != ExecutionState.IDLE) {
            logger.warn("Cannot start script - already running")
            return
        }

        logger.info("Starting script execution: ${script.name}")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStart(script.name)

        shouldStop = false
        currentContext = ExecutionContext(
            script = script,
            state = ExecutionState.RUNNING
        )

        executionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        executionScope?.launch {
            executeInfiniteLoop()
        }
    }

    fun pause() {
        if (currentContext.state == ExecutionState.RUNNING) {
            logger.info("Pausing script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionPause()
            currentContext = currentContext.copy(state = ExecutionState.PAUSED)
        }
    }

    fun resume() {
        if (currentContext.state == ExecutionState.PAUSED) {
            logger.info("Resuming script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionResume()
            currentContext = currentContext.copy(state = ExecutionState.RUNNING)
        }
    }

    fun stop() {
        logger.info("Stopping script execution")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStop()
        shouldStop = true
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
        executionScope?.cancel()
        stepExecutionOrchestrator.stop()
    }

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(currentContext.state.name)

    private suspend fun executeInfiniteLoop() {
        while (!shouldStop && currentContext.state != ExecutionState.STOPPED) {
            if (currentContext.state == ExecutionState.PAUSED) {
                delay(100)
                continue
            }

            stepExecutionOrchestrator.execute(currentContext.script.steps, currentContext) { shouldStop }
        }

        if (currentContext.state != ExecutionState.STOPPED) {
            currentContext = currentContext.copy(state = ExecutionState.IDLE)
        }
    }
}
