package com.adaptibot.execution.domain

import com.adaptibot.execution.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.execution.domain.observer.ObserverRegistry
import com.adaptibot.model.ActionStep
import com.adaptibot.model.BlockStep
import com.adaptibot.model.ObserverStep
import com.adaptibot.model.Script
import com.adaptibot.model.Step

internal class ScriptInterpreter(
    private val actionStepHandler: ActionStepHandler,
    private val blockStepResolver: BlockStepResolver,
    private val observerRegistry: ObserverRegistry,
    private val scriptExecutionState: ScriptExecutionState,
    private val observerInterruptCoordinator: ObserverInterruptCoordinator
) {
    init {
        observerInterruptCoordinator.setExecuteSequenceCallback { steps ->
            executeSteps(steps)
        }

        observerRegistry.setOnObserverTriggered { observer ->
            observerInterruptCoordinator.queueObserver(observer)
        }
    }

    fun interpret(script: Script) {
        try {
            while (scriptExecutionState.isRunning() && !Thread.currentThread().isInterrupted) {
                executeSteps(script.steps)
            }
        } finally {
            observerRegistry.clearAll()
            scriptExecutionState.completeExecution()
        }
    }

    private fun executeSteps(steps: List<Step>) {
        observerRegistry.enterScope()
        steps.forEach { step ->
            observerInterruptCoordinator.processObserverInterrupt()
            executeStep(step)
        }
        observerRegistry.exitScope()
    }

    private fun executeStep(step: Step) {
        if (scriptExecutionState.isRunning()) {
            scriptExecutionState.recordActiveStep(step)
            waitForDelay(step.delayBefore)

            when (step) {
                is ActionStep -> actionStepHandler.execute(step)
                is BlockStep -> executeSteps(blockStepResolver.resolveNestedSteps(step))
                is ObserverStep -> observerRegistry.registerObserver(step)
            }
        }
    }

    private fun waitForDelay(delayMs: Long) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}

