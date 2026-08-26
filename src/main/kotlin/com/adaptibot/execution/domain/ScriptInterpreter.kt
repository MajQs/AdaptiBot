package com.adaptibot.execution.domain

import com.adaptibot.execution.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.execution.domain.observer.ObserverRegistry
import com.adaptibot.script.Script
import com.adaptibot.script.ScriptSettings
import com.adaptibot.script.step.ActionStep
import com.adaptibot.script.step.ConditionalStep
import com.adaptibot.script.step.ForStep
import com.adaptibot.script.step.GroupStep
import com.adaptibot.script.step.ObserverStep
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.WhileStep
import org.slf4j.LoggerFactory

internal class ScriptInterpreter(
    private val actionStepHandler: ActionStepHandler,
    private val conditionEvaluator: ConditionEvaluator,
    private val observerRegistry: ObserverRegistry,
    private val scriptExecutionState: ScriptExecutionState,
    private val observerInterruptCoordinator: ObserverInterruptCoordinator
) {

    private val logger = LoggerFactory.getLogger(ScriptInterpreter::class.java)

    init {
        observerInterruptCoordinator.setExecuteSequenceCallback { steps -> executeSteps(steps) }
        observerRegistry.setOnObserverTriggered { observer -> observerInterruptCoordinator.queueObserver(observer) }
    }

    fun interpret(script: Script) {
        applySettings(script.settings)
        try {
            while (isActive()) {
                executeSteps(script.steps)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.info("Script execution interrupted by a stop request")
        } finally {
            observerRegistry.clearAll()
            actionStepHandler.releaseHeldInputs()
            scriptExecutionState.completeExecution()
        }
    }

    private fun applySettings(settings: ScriptSettings) {
        observerRegistry.checkDelayMs = settings.observerCheckDelay
    }

    private fun executeSteps(steps: List<Step>) {
        observerRegistry.enterScope()
        steps.forEach { step ->
            if (!isActive()) return@forEach
            observerInterruptCoordinator.processObserverInterrupt()
            executeStep(step)
        }
        observerRegistry.exitScope()
    }

    private fun executeStep(step: Step) {
        if (!isActive() || !step.enabled) return
        scriptExecutionState.recordActiveStep(step)
        when (step) {
            is ActionStep -> actionStepHandler.execute(step)
            is GroupStep -> executeSteps(step.container.steps)
            is ConditionalStep -> executeSteps(if (conditionEvaluator.evaluate(step.condition)) step.trueContainer.steps else step.elseContainer.steps)
            is WhileStep -> while (isActive() && conditionEvaluator.evaluate(step.condition)) { executeSteps(step.container.steps) }
            is ForStep -> repeat(step.iterations) { if (isActive()) executeSteps(step.container.steps) }
            is ObserverStep -> observerRegistry.activateObserver(step)
        }
    }

    /** A stop request interrupts the execution thread, so both signals must be checked. */
    private fun isActive(): Boolean =
        scriptExecutionState.isRunning() && !Thread.currentThread().isInterrupted
}
