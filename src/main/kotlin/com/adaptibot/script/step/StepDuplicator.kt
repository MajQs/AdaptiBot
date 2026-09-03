package com.adaptibot.script.step

internal object StepDuplicator {

    private const val COPY_SUFFIX = " (copy)"

    fun duplicate(step: Step, renameLabel: Boolean = true): Step {
        val newLabel = if (renameLabel) step.label?.plus(COPY_SUFFIX) else step.label
        return when (step) {
            is ActionStep -> step.copy(id = StepId(), label = newLabel)
            is GroupStep -> step.copy(
                id = StepId(),
                label = newLabel,
                container = duplicateContainer(step.container)
            )
            is ObserverStep -> step.copy(
                id = StepId(),
                label = newLabel,
                container = duplicateContainer(step.container)
            )
            is WhileStep -> step.copy(
                id = StepId(),
                label = newLabel,
                container = duplicateContainer(step.container)
            )
            is ForStep -> step.copy(
                id = StepId(),
                label = newLabel,
                container = duplicateContainer(step.container)
            )
            is ConditionalStep -> step.copy(
                id = StepId(),
                label = newLabel,
                trueContainer = duplicateContainer(step.trueContainer),
                elseContainer = duplicateContainer(step.elseContainer)
            )
        }
    }

    fun duplicateContainer(container: StepContainer): StepContainer = StepContainer(
        id = ContainerId(),
        steps = container.steps.map { duplicate(it, renameLabel = false) }
    )
}

