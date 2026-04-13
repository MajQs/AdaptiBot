package com.adaptibot.script.step

internal object StepTreeEditor {

    // ── Find by StepId ────────────────────────────────────────────────────────

    fun find(container: StepContainer, id: StepId): Step? {
        for (step in container.steps) {
            if (step.id == id) return step
            for (child in step.containers()) {
                find(child, id)?.let { return it }
            }
        }
        return null
    }

    // ── Find by ContainerId ───────────────────────────────────────────────────

    fun findContainer(root: StepContainer, id: ContainerId): StepContainer? {
        if (root.id == id) return root
        for (step in root.steps) {
            for (child in step.containers()) {
                findContainer(child, id)?.let { return it }
            }
        }
        return null
    }

    // ── Remove ────────────────────────────────────────────────────────────────

    fun remove(container: StepContainer, id: StepId): Boolean {
        val idx = container.steps.indexOfFirst { it.id == id }
        if (idx >= 0) { container.steps.removeAt(idx); return true }
        for (step in container.steps) {
            for (child in step.containers()) {
                if (remove(child, id)) return true
            }
        }
        return false
    }

    // ── Replace ───────────────────────────────────────────────────────────────

    fun replace(container: StepContainer, updated: Step): Boolean {
        val idx = container.steps.indexOfFirst { it.id == updated.id }
        if (idx >= 0) { container.steps[idx] = updated; return true }
        for (step in container.steps) {
            for (child in step.containers()) {
                if (replace(child, updated)) return true
            }
        }
        return false
    }

    // ── Insert after ──────────────────────────────────────────────────────────

    fun insertAfter(container: StepContainer, afterId: StepId, newStep: Step): Boolean {
        val idx = container.steps.indexOfFirst { it.id == afterId }
        if (idx >= 0) { container.steps.add(idx + 1, newStep); return true }
        for (step in container.steps) {
            for (child in step.containers()) {
                if (insertAfter(child, afterId, newStep)) return true
            }
        }
        return false
    }


    // ── Insert at position ────────────────────────────────────────────────────

    fun insertAt(root: StepContainer, step: Step, containerId: ContainerId, index: Int): Boolean {
        val target = findContainer(root, containerId) ?: return false
        target.steps.add(index.coerceIn(0, target.steps.size), step)
        return true
    }
}
