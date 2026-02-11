package com.adaptibot.core.domain

import com.adaptibot.common.model.*
import com.adaptibot.core.domain.observer.ObserverManager
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

internal class StepExecutionOrchestrator(
    private val actionExecutor: ActionExecutor,
    private val blockStepResolver: BlockStepResolver,
    private val observerManager: ObserverManager,
    private val executionController: ExecutionController
) {
    private val logger = LoggerFactory.getLogger(StepExecutionOrchestrator::class.java)
    private val triggeredObserver = AtomicReference<ObserverStep?>(null)

    init {
        observerManager.setOnObserverTriggered { observer ->
            triggeredObserver.set(observer)
            logger.info("Observer queued for execution: ${observer.id.value}")
        }
    }

    suspend fun execute(steps: List<Step>) {
        observerManager.enterScope()
        try {
            for (step in steps) {
                handleTriggeredObserver()
                if (executionController.isStopped()) {
                    break
                }
                executeStep(step)
            }
        } finally {
            observerManager.exitScope()
        }
    }

    private suspend fun executeStep(step: Step) {
        executionController.setActiveStep(step)
        if (step.delayBefore > 0) delay(step.delayBefore)

        when (step) {
            is ActionStep -> handleActionStep(step)
            is BlockStep -> handleBlockStep(step)
            is ObserverStep -> handleObserverStep(step)
        }

        if (step.delayAfter > 0) delay(step.delayAfter)
    }

    private fun handleActionStep(step: ActionStep) {
        actionExecutor.execute(step)
    }

    private suspend fun handleBlockStep(step: BlockStep) {
        val nestedSteps = blockStepResolver.resolve(step)
        execute(nestedSteps)
    }

    private fun handleObserverStep(step: ObserverStep) {
        observerManager.registerObserver(step)
    }

    private suspend fun handleTriggeredObserver() {
        triggeredObserver.getAndSet(null)?.let { observer ->
            logger.info("Executing triggered observer: ${observer.id.value}")
            try {
                execute(observer.actionSteps)
            } finally {
                logger.info("Observer execution completed, resuming from interrupted step")
            }
        }
    }

    fun stop() {
        observerManager.clearAll()
    }
}
