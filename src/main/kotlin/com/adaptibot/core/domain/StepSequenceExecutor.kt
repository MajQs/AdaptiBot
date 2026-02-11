package com.adaptibot.core.domain

import com.adaptibot.common.model.*
import com.adaptibot.core.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.core.domain.observer.ObserverRegistry
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

/**
 * Executes sequences of steps with support for blocks and observers.
 * Coordinates step execution, observer triggers, and flow control.
 */
internal class StepSequenceExecutor(
    private val actionStepHandler: ActionStepHandler,
    private val blockStepResolver: BlockStepResolver,
    private val observerRegistry: ObserverRegistry,
    private val executionSession: ExecutionSession,
    private val observerInterruptCoordinator: ObserverInterruptCoordinator
) {
    private val logger = LoggerFactory.getLogger(StepSequenceExecutor::class.java)

    init {
        // Set callback for executing sequences when observers are triggered
        observerInterruptCoordinator.setExecuteSequenceCallback { steps ->
            executeSequence(steps)
        }

        // Set callback for when observers are triggered
        observerRegistry.setOnObserverTriggered { observer ->
            observerInterruptCoordinator.queueObserver(observer)
        }
    }

    suspend fun executeSequence(steps: List<Step>) {
        observerRegistry.enterScope()
        try {
            for (step in steps) {
                observerInterruptCoordinator.processObserverInterrupt()
                if (executionSession.isStopped()) {
                    break
                }
                executeStep(step)
            }
        } finally {
            observerRegistry.exitScope()
        }
    }

    private suspend fun executeStep(step: Step) {
        executionSession.recordActiveStep(step)
        waitForDelay(step.delayBefore)

        when (step) {
            is ActionStep -> handleActionStep(step)
            is BlockStep -> handleBlockStep(step)
            is ObserverStep -> handleObserverStep(step)
        }

        waitForDelay(step.delayAfter)
    }

    private suspend fun waitForDelay(delayMs: Long) {
        if (delayMs > 0) {
            delay(delayMs)
        }
    }

    private fun handleActionStep(step: ActionStep) {
        actionStepHandler.execute(step)
    }

    private suspend fun handleBlockStep(step: BlockStep) {
        val nestedSteps = blockStepResolver.resolve(step)
        executeSequence(nestedSteps)
    }

    private fun handleObserverStep(step: ObserverStep) {
        observerRegistry.registerObserver(step)
    }

    fun stop() {
        observerRegistry.clearAll()
    }
}

