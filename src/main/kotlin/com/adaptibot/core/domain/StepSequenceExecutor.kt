package com.adaptibot.core.domain

import com.adaptibot.common.model.ActionStep
import com.adaptibot.common.model.BlockStep
import com.adaptibot.common.model.ObserverStep
import com.adaptibot.common.model.Step
import com.adaptibot.core.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.core.domain.observer.ObserverRegistry

internal class StepSequenceExecutor(
    private val actionStepHandler: ActionStepHandler,
    private val blockStepResolver: BlockStepResolver,
    private val observerRegistry: ObserverRegistry,
    private val executionSession: ExecutionSession,
    private val observerInterruptCoordinator: ObserverInterruptCoordinator
) {
    init {
        observerInterruptCoordinator.setExecuteSequenceCallback { steps ->
            executeSequence(steps)
        }

        observerRegistry.setOnObserverTriggered { observer ->
            observerInterruptCoordinator.queueObserver(observer)
        }
    }

    fun executeSequence(steps: List<Step>) {
        observerRegistry.enterScope()
        try {
            for (step in steps) {
                observerInterruptCoordinator.processObserverInterrupt()
                if (executionSession.isStopped() || Thread.currentThread().isInterrupted) {
                    break
                }
                executeStep(step)
            }
        } finally {
            observerRegistry.exitScope()
        }
    }

    private fun executeStep(step: Step) {
        executionSession.recordActiveStep(step)
        waitForDelay(step.delayBefore)

        when (step) {
            is ActionStep -> actionStepHandler.execute(step)
            is BlockStep -> executeSequence(blockStepResolver.resolveNestedSteps(step))
            is ObserverStep -> observerRegistry.registerObserver(step)
        }

        waitForDelay(step.delayAfter)
    }

    private fun waitForDelay(delayMs: Long) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                // Gracefully handle interruption - execution will stop at next check
            }
        }
    }

    fun stop() {
        observerRegistry.clearAll()
    }
}

