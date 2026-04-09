package com.adaptibot.script.step

/**
 * Internal helper that encapsulates all recursive tree-traversal operations on a mutable
 * list of [Step]s. Used exclusively by [Script] to implement its domain methods.
 *
 * All functions operate on a plain [MutableList] – no JavaFX dependency, fully unit-testable.
 */
internal object StepTreeEditor {

    fun find(steps: List<Step>, id: StepId): Step? {
        for (step in steps) {
            if (step.id == id) return step
            val nested: Step? = when (step) {
                is ConditionalBlock -> find(step.steps, id) ?: find(step.elseSteps, id)
                is BlockStep        -> find(step.steps, id)
                is ObserverStep     -> find(step.steps, id)
                else                -> null
            }
            if (nested != null) return nested
        }
        return null
    }

    fun remove(steps: MutableList<Step>, id: StepId): Boolean {
        val idx = steps.indexOfFirst { it.id == id }
        if (idx >= 0) { steps.removeAt(idx); return true }
        return recurse(steps) { step, nested ->
            if (remove(nested, id)) step.withUpdatedChildren(nested) else null
        }
    }

    fun replace(steps: MutableList<Step>, updated: Step): Boolean {
        val idx = steps.indexOfFirst { it.id == updated.id }
        if (idx >= 0) { steps[idx] = updated; return true }
        return recurse(steps) { step, nested ->
            if (replace(nested, updated)) step.withUpdatedChildren(nested) else null
        }
    }

    fun insertAfter(steps: MutableList<Step>, afterId: StepId, newStep: Step): Boolean {
        val idx = steps.indexOfFirst { it.id == afterId }
        if (idx >= 0) { steps.add(idx + 1, newStep); return true }
        return recurse(steps) { step, nested ->
            if (insertAfter(nested, afterId, newStep)) step.withUpdatedChildren(nested) else null
        }
    }

    fun addToChildren(steps: MutableList<Step>, parentId: StepId, step: Step): Boolean {
        for (i in steps.indices) {
            val current = steps[i]
            if (current.id == parentId) {
                val updated = current.withAppendedChild(step) ?: return false
                steps[i] = updated
                return true
            }
        }
        return recurse(steps) { current, nested ->
            if (addToChildren(nested, parentId, step)) current.withUpdatedChildren(nested) else null
        }
    }

    fun addToElse(steps: MutableList<Step>, parentId: StepId, step: Step): Boolean {
        for (i in steps.indices) {
            val current = steps[i]
            if (current is ConditionalBlock && current.id == parentId) {
                steps[i] = current.copy(elseSteps = current.elseSteps + step)
                return true
            }
        }
        return recurse(steps) { current, nested ->
            if (addToElse(nested, parentId, step)) current.withUpdatedChildren(nested) else null
        }
    }

    fun insertAt(steps: MutableList<Step>, step: Step, parentId: StepId?, index: Int): Boolean {
        if (parentId == null) {
            steps.add(index.coerceIn(0, steps.size), step)
            return true
        }
        return insertIntoParent(steps, step, parentId, index)
    }

    private fun insertIntoParent(steps: MutableList<Step>, step: Step, parentId: StepId, index: Int): Boolean {
        for (i in steps.indices) {
            val current = steps[i]
            if (current.id == parentId) {
                val newList = current.childSteps().toMutableList()
                    .also { it.add(index.coerceIn(0, it.size), step) }
                val updated = current.withUpdatedChildren(newList) ?: return false
                steps[i] = updated
                return true
            }
        }
        return recurse(steps) { current, nested ->
            if (insertIntoParent(nested, step, parentId, index)) current.withUpdatedChildren(nested) else null
        }
    }

    /**
     * Iterates over [steps], and for each container step calls [transform] with a mutable copy
     * of its primary children list. If [transform] returns a non-null replacement step,
     * that replacement is written back and the function returns `true`.
     *
     * For [ConditionalBlock] the elseSteps branch is tried as a fallback when the primary branch
     * yields no match.
     */
    private inline fun recurse(
        steps: MutableList<Step>,
        transform: (Step, MutableList<Step>) -> Step?
    ): Boolean {
        for (i in steps.indices) {
            val current = steps[i]
            val primaryChildren = current.childSteps().toMutableList()
            val replacement = transform(current, primaryChildren)
            if (replacement != null) { steps[i] = replacement; return true }

            // ConditionalBlock has a second branch – elseSteps
            if (current is ConditionalBlock) {
                val elseChildren = current.elseSteps.toMutableList()
                val elseReplacement = transform(current, elseChildren)
                if (elseReplacement != null) {
                    steps[i] = current.copy(elseSteps = elseChildren)
                    return true
                }
            }
        }
        return false
    }
}

// ── Step extension helpers ────────────────────────────────────────────────────

/** Returns the primary (non-else) children of a container step, or empty list for leaf steps. */
private fun Step.childSteps(): List<Step> = when (this) {
    is ConditionalBlock -> steps
    is BlockStep        -> steps
    is ObserverStep     -> steps
    else                -> emptyList()
}

/**
 * Returns a copy of this step with [newChildren] as its primary children list,
 * or `null` if this step type does not support children.
 */
private fun Step.withUpdatedChildren(newChildren: List<Step>): Step? = when (this) {
    is GroupBlock       -> copy(steps = newChildren)
    is ConditionalBlock -> copy(steps = newChildren)
    is ObserverStep     -> copy(steps = newChildren)
    else                -> null
}

/**
 * Returns a copy of this step with [newStep] appended to its primary children list,
 * or `null` if this step type does not support children (or does not accept appending).
 */
private fun Step.withAppendedChild(newStep: Step): Step? = when (this) {
    is BlockStep    -> withUpdatedChildren(steps + newStep)
    is ObserverStep -> copy(steps = steps + newStep)
    else            -> null
}

