package com.adaptibot.core.domain

import com.adaptibot.common.model.Step
import com.adaptibot.core.domain.observer.ObserverManager
import com.adaptibot.core.dto.ExecutionContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

internal class StepExecutionOrchestrator(
    private val stepExecutor: StepExecutor,
    private val observerManager: ObserverManager
) {
    private val logger = LoggerFactory.getLogger(StepExecutionOrchestrator::class.java)
    private val triggeredObserver = AtomicReference<Step.ObserverBlock?>(null)

    init {
        observerManager.setOnObserverTriggered { observer ->
            triggeredObserver.set(observer)
            logger.info("Observer queued for execution: ${observer.id.value}")
        }
    }

    suspend fun execute(steps: List<Step>, context: ExecutionContext, shouldStop: () -> Boolean): List<Step.ObserverBlock> {
        val registeredObservers = mutableListOf<Step.ObserverBlock>()
        for (step in steps) {
            if (shouldStop() || context.state == com.adaptibot.core.dto.ExecutionState.PAUSED) {
                break
            }
            registeredObservers.addAll(executeStep(step, context, shouldStop))
        }
        registeredObservers.forEach { observerManager.unregisterObserver(it) }
        return registeredObservers
    }

    private suspend fun executeStep(step: Step, context: ExecutionContext, shouldStop: () -> Boolean): List<Step.ObserverBlock> {
        checkAndExecuteTriggeredObserver(context, shouldStop)

        val currentContext = context.copy(activeStep = step)
        val (nestedSteps, registeredObservers) = stepExecutor.execute(step, currentContext, shouldStop)

        if (nestedSteps.isNotEmpty()) {
            val nestedRegisteredObservers = execute(nestedSteps, currentContext, shouldStop)
            nestedRegisteredObservers.forEach { observerManager.unregisterObserver(it) }
        }

        if (step is Step.ObserverBlock) {
            observerManager.registerObserver(step)
            return registeredObservers + step
        }

        return registeredObservers
    }

    private suspend fun checkAndExecuteTriggeredObserver(context: ExecutionContext, shouldStop: () -> Boolean) {
        val observer = triggeredObserver.getAndSet(null)
        if (observer != null) {
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

