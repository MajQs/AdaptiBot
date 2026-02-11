package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.dto.ExecutionStateDto
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

internal class ScriptOrchestrator(
    private val stepExecutionOrchestrator: StepExecutionOrchestrator,
    private val executionController: ExecutionController,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ScriptOrchestrator::class.java)

    fun start(script: Script) {
        if ( !executionController.isIdle()) {
            logger.warn("Cannot start script - already running")
            return
        }

        logger.info("Starting script execution: ${script.name}")
        eventPublisher.logExecutionStart(script.name)

        executionController.start(script)

        executionController.getScope()?.launch {
            executeInfiniteLoop(script)
        }
    }

    fun stop() {
        logger.info("Stopping script execution")
        eventPublisher.logExecutionStop()
        executionController.stop()
        stepExecutionOrchestrator.stop()
    }

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(executionController.getContext().state.name)

    private suspend fun executeInfiniteLoop(script: Script) {
        while (executionController.isRunning()) {
            stepExecutionOrchestrator.execute(script.steps)
        }
        executionController.finish()
    }
}
