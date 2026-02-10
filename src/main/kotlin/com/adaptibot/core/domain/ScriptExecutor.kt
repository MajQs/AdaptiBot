package com.adaptibot.core.domain

import com.adaptibot.common.model.Script
import com.adaptibot.common.model.Step
import com.adaptibot.core.dto.ExecutionStateDto
import com.adaptibot.core.domain.actions.ActionExecutor
import com.adaptibot.core.domain.actions.ConditionEvaluator
import com.adaptibot.core.domain.actions.ElementFinder
import com.adaptibot.core.domain.observer.ObserverManager
import com.adaptibot.core.dto.ExecutionContext
import com.adaptibot.core.dto.ExecutionState
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference

internal class ScriptExecutor(
    private val stepExecutor: StepExecutor,
    private val observerManager: ObserverManager,
) {

    private val logger = LoggerFactory.getLogger(ScriptExecutor::class.java)

    private var executionScope: CoroutineScope? = null
    private var currentContext: ExecutionContext = ExecutionContext(
        script = Script("", steps = emptyList()),
        state = ExecutionState.IDLE
    )

    @Volatile
    private var shouldStop = false

    private val triggeredObserver = AtomicReference<Step.ObserverBlock?>(null)

    fun start(script: Script) {
        if (currentContext.state != ExecutionState.IDLE) {
            logger.warn("Cannot start script - already running")
            return
        }

        logger.info("Starting script execution: ${script.name}")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStart(script.name)

        shouldStop = false
        triggeredObserver.set(null)
        currentContext = ExecutionContext(
            script = script,
            state = ExecutionState.RUNNING
        )

        observerManager.setOnObserverTriggered { observer ->
            triggeredObserver.set(observer)
            logger.info("Observer queued for execution: ${observer.id.value}")
        }

        executionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        executionScope?.launch {
            executeInfiniteLoop()
        }
    }

    fun pause() {
        if (currentContext.state == ExecutionState.RUNNING) {
            logger.info("Pausing script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionPause()
            currentContext = currentContext.copy(state = ExecutionState.PAUSED)
        }
    }

    fun resume() {
        if (currentContext.state == ExecutionState.PAUSED) {
            logger.info("Resuming script execution")
            com.adaptibot.ui.model.ExecutionLogger.logExecutionResume()
            currentContext = currentContext.copy(state = ExecutionState.RUNNING)
        }
    }

    fun stop() {
        logger.info("Stopping script execution")
        com.adaptibot.ui.model.ExecutionLogger.logExecutionStop()
        shouldStop = true
        currentContext = currentContext.copy(state = ExecutionState.STOPPED)
        executionScope?.cancel()
        observerManager.clearAll()
    }

    fun getState(): ExecutionStateDto = ExecutionStateDto.valueOf(currentContext.state.name)

    private suspend fun executeInfiniteLoop() {
        while (!shouldStop && currentContext.state != ExecutionState.STOPPED) {
            if (currentContext.state == ExecutionState.PAUSED) {
                delay(100)
                continue
            }

            executeIteration()

        }

        // Only set to IDLE if not explicitly stopped
        if (currentContext.state != ExecutionState.STOPPED) {
            currentContext = currentContext.copy(state = ExecutionState.IDLE)
        }
    }

    private suspend fun executeIteration() {
        currentContext.script.steps.forEach { step ->
            if (shouldStop || currentContext.state == ExecutionState.PAUSED) {
                return
            }
            executeStep(step)
        }
    }

    private suspend fun executeStep(step: Step) {
        checkAndExecuteTriggeredObserver(step.id)

        currentContext = currentContext.copy(currentStepId = step.id)

        stepExecutor.execute(step, currentContext) { shouldStop }
    }

    private suspend fun checkAndExecuteTriggeredObserver(currentStepId: com.adaptibot.common.model.StepId) {
        val observer = triggeredObserver.getAndSet(null)
        if (observer != null) {
            logger.info("Executing triggered observer: ${observer.id.value}, interrupting step: ${currentStepId.value}")

            currentContext = currentContext.copy(
                interruptedStepId = currentStepId,
                isExecutingObserver = true
            )

            try {
                for (actionStep in observer.actionSteps) {
                    if (shouldStop || currentContext.state == ExecutionState.PAUSED) {
                        break
                    }
                    stepExecutor.execute(actionStep, currentContext) { shouldStop }
                }
            } finally {
                currentContext = currentContext.copy(
                    interruptedStepId = null,
                    isExecutingObserver = false
                )
                logger.info("Observer execution completed, resuming from interrupted step")
            }
        }
    }
}

