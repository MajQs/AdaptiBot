package com.adaptibot.core.domain

import com.adaptibot.common.model.*
import com.adaptibot.core.domain.observer.ObserverManager
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

internal class StepExecutionOrchestrator(
    private val actionExecutor: ActionExecutor,
    private val blockExecutor: BlockExecutor,
    private val observerManager: ObserverManager
) {
    private val logger = LoggerFactory.getLogger(StepExecutionOrchestrator::class.java)
    private val triggeredObserver = AtomicReference<ObserverStep?>(null)

    init {
        observerManager.setOnObserverTriggered { observer ->
            triggeredObserver.set(observer)
            logger.info("Observer queued for execution: ${observer.id.value}")
        }
    }

    suspend fun execute(steps: List<Step>, context: ExecutionContext, shouldStop: () -> Boolean) {
        observerManager.enterScope()
        try {
            for (step in steps) {
                handleTriggeredObserver(context, shouldStop)

                if (shouldStop() || context.state == ExecutionState.PAUSED) {
                    break
                }
                executeStep(step, context, shouldStop)
            }
        } finally {
            observerManager.exitScope()
        }
    }

    private suspend fun executeStep(step: Step, context: ExecutionContext, shouldStop: () -> Boolean) {
        if (shouldStop() || context.state == ExecutionState.PAUSED) return

        if (step.delayBefore > 0) delay(step.delayBefore)

        when (step) {
            is ActionStep -> handleActionStep(step, shouldStop)
            is BlockStep -> handleBlockStep(step, context, shouldStop)
            is ObserverStep -> handleObserverStep(step)
        }

        if (step.delayAfter > 0) delay(step.delayAfter)
    }

    private suspend fun handleActionStep(step: ActionStep, shouldStop: () -> Boolean) {
        actionExecutor.execute(step, shouldStop)
    }

    private suspend fun handleBlockStep(step: BlockStep, context: ExecutionContext, shouldStop: () -> Boolean) {
        val nestedSteps = blockExecutor.execute(step)
        val currentContext = context.copy(activeStep = step)
        execute(nestedSteps, currentContext, shouldStop)
    }

    private fun handleObserverStep(step: ObserverStep) {
        observerManager.registerObserver(step)
    }

    private suspend fun handleTriggeredObserver(context: ExecutionContext, shouldStop: () -> Boolean) {
        triggeredObserver.getAndSet(null)?.let { observer ->
            logger.info("Executing triggered observer: ${observer.id.value}")
            val observerContext = context.copy(activeStep = observer)
            try {
                execute(observer.actionSteps, observerContext, shouldStop)
            } finally {
                logger.info("Observer execution completed, resuming from interrupted step")
            }
        }
    }

    fun stop() {
        observerManager.clearAll()
    }
}
