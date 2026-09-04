package com.adaptibot.script

import com.adaptibot.script.step.ActionStep
import com.adaptibot.script.step.ObserverStep
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.containers

/**
 * Static checks reported to the user when a script is saved. They never block saving - they point
 * out constructs that behave in a way the user probably did not intend.
 */
object ScriptValidator {

    fun warnings(script: Script): List<String> = script.steps.flatMap { warningsFor(it) }

    private fun warningsFor(step: Step): List<String> {
        val nested = step.containers().flatMap { container -> container.steps.flatMap { warningsFor(it) } }
        return if (step is ObserverStep) observerWarnings(step) + nested else nested
    }

    /**
     * An observer is re-armed as soon as its steps finish, so a handler that cannot change what the
     * observer sees will trigger again on the very next check.
     */
    private fun observerWarnings(observer: ObserverStep): List<String> {
        val name = observer.label?.takeIf { it.isNotBlank() } ?: "Observer"
        val handlerSteps = observer.container.steps

        return when {
            handlerSteps.isEmpty() -> listOf(
                "Observer \"$name\" has no steps - while its condition stays true it will trigger on " +
                        "every check. Add steps that make the watched element disappear, or a Wait action."
            )

            handlerSteps.none { containsAction(it) } -> listOf(
                "Observer \"$name\" performs no action - while its condition stays true it will trigger " +
                        "on every check. Add steps that make the watched element disappear, or a Wait action."
            )

            else -> emptyList()
        }
    }

    private fun containsAction(step: Step): Boolean =
        step is ActionStep || step.containers().any { container -> container.steps.any { containsAction(it) } }
}

