package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.dto.ExecutionStateDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

internal class ScriptExecutionService(
    private val stepSequenceExecutor: StepSequenceExecutor,
    private val executionSession: ExecutionSession,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ScriptExecutionService::class.java)

    private var executionScope: CoroutineScope? = null

    init {
        executionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }

    fun start(script: Script) {
        if (!executionSession.isIdle()) {
            logger.warn("Cannot start script - already running")
            return
        }

        logger.info("Starting script execution: ${script.name}")
        eventPublisher.logExecutionStart(script.name)

        executionSession.start(script)

        executionScope?.launch(block = { executeScriptLoop(script) })

    }

    fun stop() {
        logger.info("Stopping script execution")
        eventPublisher.logExecutionStop()
        executionSession.stop()
        stepSequenceExecutor.stop()
    }

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(executionSession.getContext().state.name)

    private suspend fun executeScriptLoop(script: Script) {
        while (executionSession.isRunning()) {
            stepSequenceExecutor.executeSequence(script.steps)
        }
        executionSession.completeExecution()
    }
}

