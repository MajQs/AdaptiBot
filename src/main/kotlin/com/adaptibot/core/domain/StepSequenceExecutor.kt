package com.adaptibot.core.domain

import com.adaptibot.common.model.*
import com.adaptibot.core.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.core.domain.observer.ObserverRegistry
import kotlinx.coroutines.delay

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
            is ActionStep -> actionStepHandler.execute(step)
            is BlockStep -> executeSequence(blockStepResolver.resolveNestedSteps(step))
            is ObserverStep -> observerRegistry.registerObserver(step)
        }

        waitForDelay(step.delayAfter)
    }

    private suspend fun waitForDelay(delayMs: Long) {
        if (delayMs > 0) {
            delay(delayMs)
        }
    }

    fun stop() {
        observerRegistry.clearAll()
    }
}

