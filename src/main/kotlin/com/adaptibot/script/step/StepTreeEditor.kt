package com.adaptibot.script.step

internal object StepTreeEditor {

    fun find(container: StepContainer, id: StepId): Step? {
        for (step in container.steps) {
            if (step.id == id) return step
            for (child in step.containers()) {
                find(child, id)?.let { return it }
            }
        }
        return null
    }

    fun findContainer(root: StepContainer, id: ContainerId): StepContainer? {
        if (root.id == id) return root
        for (step in root.steps) {
            for (child in step.containers()) {
                findContainer(child, id)?.let { return it }
            }
        }
        return null
    }

    fun remove(container: StepContainer, id: StepId): StepContainer? {
        val directIdx = container.steps.indexOfFirst { it.id == id }
        if (directIdx >= 0) {
            return container.copy(steps = container.steps.toMutableList().also { it.removeAt(directIdx) })
        }
        return rebuildWithStepUpdate(container) { step ->
            step.containers().firstNotNullOfOrNull { child ->
                remove(child, id)?.let { step.withUpdatedContainer(child, it) }
            }
        }
    }

    fun replace(container: StepContainer, updated: Step): StepContainer? {
        val directIdx = container.steps.indexOfFirst { it.id == updated.id }
        if (directIdx >= 0) {
            val newSteps = container.steps.toMutableList().also { it[directIdx] = updated }
            return container.copy(steps = newSteps)
        }
        return rebuildWithStepUpdate(container) { step ->
            step.containers().firstNotNullOfOrNull { child ->
                replace(child, updated)?.let { step.withUpdatedContainer(child, it) }
            }
        }
    }

    fun insertAfter(container: StepContainer, afterId: StepId, newStep: Step): StepContainer? {
        val directIdx = container.steps.indexOfFirst { it.id == afterId }
        if (directIdx >= 0) {
            val newSteps = container.steps.toMutableList().also { it.add(directIdx + 1, newStep) }
            return container.copy(steps = newSteps)
        }
        return rebuildWithStepUpdate(container) { step ->
            step.containers().firstNotNullOfOrNull { child ->
                insertAfter(child, afterId, newStep)?.let { step.withUpdatedContainer(child, it) }
            }
        }
    }

    fun insertAt(root: StepContainer, step: Step, containerId: ContainerId, index: Int): StepContainer? {
        return rebuildWithContainerUpdate(root, containerId) { target ->
            val newSteps = target.steps.toMutableList().also {
                it.add(index.coerceIn(0, it.size), step)
            }
            target.copy(steps = newSteps)
        }
    }

    private fun rebuildWithContainerUpdate(
        container: StepContainer,
        targetId: ContainerId,
        containerTransform: (StepContainer) -> StepContainer
    ): StepContainer? {
        if (container.id == targetId) return containerTransform(container)

        val newSteps = container.steps.map { step ->
            step.containers().firstNotNullOfOrNull { child ->
                rebuildWithContainerUpdate(child, targetId, containerTransform)
                    ?.let { step.withUpdatedContainer(child, it) }
            } ?: step
        }
        return if (newSteps == container.steps) null
        else container.copy(steps = newSteps)
    }

    private fun rebuildWithStepUpdate(
        container: StepContainer,
        stepTransform: (Step) -> Step?
    ): StepContainer? {
        var changed = false
        val newSteps = container.steps.map { step ->
            val updated = stepTransform(step)
            if (updated != null) { changed = true; updated } else step
        }
        return if (changed) container.copy(steps = newSteps) else null
    }
}
