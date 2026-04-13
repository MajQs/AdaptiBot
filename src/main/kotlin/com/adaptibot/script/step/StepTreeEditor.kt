package com.adaptibot.script.step

internal object StepTreeEditor {

    // ── Step-level operations ─────────────────────────────────────────────────

    fun find(steps: List<Step>, id: StepId): Step? {
        for (step in steps) {
            if (step.id == id) return step
            val nested = step.childSteps().let { find(it, id) }
            if (nested != null) return nested
        }
        return null
    }

    fun findBranch(steps: List<Step>, branchId: BranchId): Branch? {
        for (step in steps) {
            if (step is ConditionalStep) {
                if (step.trueBranch.id == branchId) return step.trueBranch
                if (step.elseBranch.id == branchId) return step.elseBranch
            }
            val nested = findBranch(step.childSteps(), branchId)
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

    fun addToBranch(steps: MutableList<Step>, branchId: BranchId, step: Step): Boolean {
        for (i in steps.indices) {
            val current = steps[i]
            if (current is ConditionalStep) {
                val updated = current.withAppendedToBranch(branchId, step)
                if (updated != null) { steps[i] = updated; return true }
            }
        }
        return recurse(steps) { current, nested ->
            if (addToBranch(nested, branchId, step)) current.withUpdatedChildren(nested) else null
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

    private inline fun recurse(
        steps: MutableList<Step>,
        transform: (Step, MutableList<Step>) -> Step?
    ): Boolean {
        for (i in steps.indices) {
            val current = steps[i]
            val children = current.childSteps().toMutableList()
            val replacement = transform(current, children)
            if (replacement != null) { steps[i] = replacement; return true }
        }
        return false
    }
}

// ── Step extension helpers ────────────────────────────────────────────────────

/** All direct child [Step]s of a container step; empty for leaf steps. */
private fun Step.childSteps(): List<Step> = when (this) {
    is GroupStep       -> steps
    is ObserverStep    -> steps
    is ConditionalStep -> trueBranch.steps + elseBranch.steps
    is WhileStep       -> steps
    is ForStep         -> steps
    is ActionStep      -> emptyList()
}

/**
 * Returns a copy of this step with [newChildren] replacing its flat child list.
 * For [ConditionalStep] the children are the concatenation of both branches;
 * this function splits them back by preserving the original branch sizes.
 */
private fun Step.withUpdatedChildren(newChildren: List<Step>): Step? = when (this) {
    is GroupStep       -> copy(steps = newChildren)
    is ObserverStep    -> copy(steps = newChildren)
    is WhileStep       -> copy(steps = newChildren)
    is ForStep         -> copy(steps = newChildren)
    is ConditionalStep -> {
        // newChildren is the merged flat list from both branches; re-split by original sizes
        val trueSize = trueBranch.steps.size
        copy(
            trueBranch = trueBranch.copy(steps = newChildren.take(trueSize)),
            elseBranch = elseBranch.copy(steps = newChildren.drop(trueSize))
        )
    }
    is ActionStep      -> null
}

/** Returns a copy with [newStep] appended to the primary steps list, or `null` for leaf steps. */
private fun Step.withAppendedChild(newStep: Step): Step? = when (this) {
    is GroupStep       -> copy(steps = steps + newStep)
    is ObserverStep    -> copy(steps = steps + newStep)
    is WhileStep       -> copy(steps = steps + newStep)
    is ForStep         -> copy(steps = steps + newStep)
    is ConditionalStep -> null  // use withAppendedToBranch instead
    is ActionStep      -> null
}

/**
 * Returns a copy of this [ConditionalStep] with [newStep] appended to the branch
 * identified by [branchId], or `null` if [branchId] does not match either branch.
 */
private fun ConditionalStep.withAppendedToBranch(branchId: BranchId, newStep: Step): ConditionalStep? =
    when (branchId) {
        trueBranch.id -> copy(trueBranch = trueBranch.copy(steps = trueBranch.steps + newStep))
        elseBranch.id -> copy(elseBranch = elseBranch.copy(steps = elseBranch.steps + newStep))
        else          -> null
    }
