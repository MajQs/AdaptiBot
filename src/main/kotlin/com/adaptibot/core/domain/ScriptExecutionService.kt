package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.core.domain.observer.ObserverRegistry
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionStateDto
import org.slf4j.LoggerFactory

internal class ScriptExecutionService(
    private val stepSequenceExecutor: StepSequenceExecutor,
    private val observerRegistry: ObserverRegistry,
    private val executionSession: ExecutionSession,
) {
    private val logger = LoggerFactory.getLogger(ScriptExecutionService::class.java)

    @Volatile
    private var executionThread: Thread? = null

    fun start(script: Script) {
        executionSession.create(script)
            .runLoop()
    }

    fun stop() {
        executionSession.stop()

        executionThread?.interrupt()
        executionThread = null
    }

    fun getExecutionState(): ExecutionStateDto = ExecutionStateDto.valueOf(executionSession.getState().name)

    private fun ExecutionContext.runLoop() {
        executionThread = Thread.ofVirtual()
            .name("script-execution")
            .start {
                executeScriptLoop(this.script)
            }
    }

    private fun executeScriptLoop(script: Script) {
        try {
            while (executionSession.isRunning() && !Thread.currentThread().isInterrupted) {
                stepSequenceExecutor.executeSequence(script.steps)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.debug("Script execution interrupted")
        } catch (e: Exception) {
            logger.error("Error during script execution", e)
        } finally {
            observerRegistry.clearAll()
            executionSession.completeExecution()
        }
    }
}

