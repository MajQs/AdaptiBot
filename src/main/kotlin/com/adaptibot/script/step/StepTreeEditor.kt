package com.adaptibot.script.step

internal object StepTreeEditor {

    fun find(steps: List<Step>, id: StepId): Step? {
        for (step in steps) {
            if (step.id == id) return step
            val nested: Step? = when (step) {
                is ConditionalStep -> find(step.ifBlock.steps, id)
                    ?: find(step.elseBlock.steps, id)
                    ?: if (step.ifBlock.id == id) step.ifBlock
                       else if (step.elseBlock.id == id) step.elseBlock
                       else null
                is BlockStep    -> find(step.steps, id)
                is ObserverStep -> find(step.steps, id)
                else            -> null
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
     * [ConditionalStep] is handled by delegating into its [IfBlock] and [ElseBlock] branches,
     * which are themselves [BlockStep]s and processed uniformly – no special case needed here.
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
        }
        return false
    }
}

// ── Step extension helpers ────────────────────────────────────────────────────

/**
 * Returns the primary children of a container step.
 * For [ConditionalStep] the "children" at this level are [IfBlock] and [ElseBlock] themselves;
 * their inner steps are handled when those blocks are visited in turn.
 */
private fun Step.childSteps(): List<Step> = when (this) {
    is ConditionalStep -> listOf(ifBlock, elseBlock)
    is BlockStep        -> steps
    is ObserverStep     -> steps
    else                -> emptyList()
}

/**
 * Returns a copy of this step with [newChildren] as its children list,
 * or `null` if this step type does not support children.
 *
 * For [ConditionalStep] [newChildren] must be exactly [IfBlock, ElseBlock].
 */
private fun Step.withUpdatedChildren(newChildren: List<Step>): Step? = when (this) {
    is GroupBlock       -> copy(steps = newChildren)
    is IfBlock          -> copy(steps = newChildren)
    is ElseBlock        -> copy(steps = newChildren)
    is ConditionalStep -> {
        val newIf   = newChildren.filterIsInstance<IfBlock>().firstOrNull()   ?: ifBlock
        val newElse = newChildren.filterIsInstance<ElseBlock>().firstOrNull() ?: elseBlock
        copy(ifBlock = newIf, elseBlock = newElse)
    }
    is ObserverStep     -> copy(steps = newChildren)
    else                -> null
}

/**
 * Returns a copy of this step with [newStep] appended to its primary children list,
 * or `null` if this step type does not support children.
 */
private fun Step.withAppendedChild(newStep: Step): Step? = when (this) {
    is BlockStep    -> withUpdatedChildren(steps + newStep)
    is ObserverStep -> copy(steps = steps + newStep)
    else            -> null
}
