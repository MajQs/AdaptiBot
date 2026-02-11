package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.dto.ExecutionState
import com.adaptibot.core.dto.ExecutionStateDto
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

internal class ScriptOrchestrator(
    private val stepExecutionOrchestrator: StepExecutionOrchestrator,
    private val executionController: ExecutionController
) {
    private val logger = LoggerFactory.getLogger(ScriptOrchestrator::class.java)

    fun start(script: Script) {
        if (executionController.getContext().state != ExecutionState.IDLE) {
            logger.warn("Cannot start script - already running")
            return
        }

        logger.info("Starting script execution: ${script.name}")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStart(script.name)

        executionController.start(script)

        executionController.getScope()?.launch {
            executeInfiniteLoop()
        }
    }

    fun pause() {
        if (executionController.getContext().state == ExecutionState.RUNNING) {
            logger.info("Pausing script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionPause()
            executionController.pause()
        }
    }

    fun resume() {
        if (executionController.getContext().state == ExecutionState.PAUSED) {
            logger.info("Resuming script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionResume()
            executionController.resume()
        }
    }

    fun stop() {
        logger.info("Stopping script execution")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStop()
        executionController.stop()
        stepExecutionOrchestrator.stop()
    }

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(executionController.getContext().state.name)

    private suspend fun executeInfiniteLoop() {
        val context = executionController.getContext()
        while (context.state != ExecutionState.STOPPED && context.state != ExecutionState.IDLE) {
            if (context.state == ExecutionState.PAUSED) {
                delay(100)
                continue
            }

            stepExecutionOrchestrator.execute(context.script.steps)
        }
        executionController.finish()
    }
}
