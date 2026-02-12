package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.domain.observer.ObserverRegistry
import com.adaptibot.core.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

internal class ScriptExecutionService(
    private val stepSequenceExecutor: StepSequenceExecutor,
    private val observerRegistry: ObserverRegistry,
    private val executionSession: ExecutionSession,
    private val eventPublisher: ExecutionEventPublisher
) {
    private val logger = LoggerFactory.getLogger(ScriptExecutionService::class.java)

    @Volatile
    private var executionThread: Thread? = null

    fun start(script: Script) {
        if (!executionSession.isIdle()) {
            logger.warn("Cannot start script - already running")
            return
        }

        logger.info("Starting script execution: ${script.name}")
        eventPublisher.logExecutionStart(script.name)

        executionSession.start(script)

        executionThread = Thread.ofVirtual()
            .name("script-execution")
            .start {
                executeScriptLoop(script)
            }
    }

    fun stop() {
        logger.info("Stopping script execution")
        eventPublisher.logExecutionStop()

        // Stop all components at the same abstraction level
        executionSession.stop()
        executionThread?.interrupt()
        executionThread = null

        // Stop observer thread - ScriptExecutionService orchestrates lifecycle of BOTH threads
        observerRegistry.clearAll()
    }

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(executionSession.getContext().state.name)

    private fun executeScriptLoop(script: Script) {
        try {
            while (executionSession.isRunning() && !Thread.currentThread().isInterrupted) {
                stepSequenceExecutor.executeSequence(script.steps)
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.debug("Script execution interrupted")
        } catch (e: Exception) {
            logger.error("Error during script execution", e)
        } finally {
            executionSession.completeExecution()
        }
    }
}

