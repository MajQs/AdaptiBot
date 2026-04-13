package com.adaptibot.execution.domain

import com.adaptibot.execution.domain.observer.ObserverInterruptCoordinator
import com.adaptibot.execution.domain.observer.ObserverRegistry
import com.adaptibot.script.Script
import com.adaptibot.script.ScriptSettings
import com.adaptibot.script.step.*
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
            while (scriptExecutionState.isRunning() && !Thread.currentThread().isInterrupted) {
                executeSteps(script.steps)
            }
        } finally {
            observerRegistry.clearAll()
            scriptExecutionState.completeExecution()
        }
    }

    private fun applySettings(settings: ScriptSettings) {
        observerRegistry.checkDelayMs = settings.observerCheckDelay
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
        if (!scriptExecutionState.isRunning()) return
        scriptExecutionState.recordActiveStep(step)
        waitForDelay(step.delayBefore)
        when (step) {
            is ActionStep -> actionStepHandler.execute(step)
            is GroupStep -> executeSteps(step.steps)
            is ConditionalStep -> executeSteps(if (conditionEvaluator.evaluate(step.condition)) step.trueBranch.steps else step.elseBranch.steps)
            is ObserverStep -> observerRegistry.activateObserver(step)
            // ── Loop steps – not yet implemented ──────────────────────────────
            is WhileStep -> throw UnsupportedOperationException("WhileStep execution not yet implemented")
            is ForStep -> throw UnsupportedOperationException("ForStep execution not yet implemented")
        }
    }

    private fun waitForDelay(delayMs: Long) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}
