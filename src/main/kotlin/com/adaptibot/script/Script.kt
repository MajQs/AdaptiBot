package com.adaptibot.script

import com.adaptibot.script.step.Branch
import com.adaptibot.script.step.BranchId
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepId
import com.adaptibot.script.step.StepTreeEditor

class Script private constructor(
    val id: ScriptId,
    name: String,
    description: String,
    steps: List<Step>,
    val settings: ScriptSettings
) {
    var name: String = name
        private set

    var description: String = description
        private set

    private val _steps: MutableList<Step> = steps.toMutableList()
    val steps: List<Step> get() = _steps.toList()

    // ── Metadata ───────────────────────────────────────────────────────────────

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Script name must not be blank" }
        name = newName
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
    }

    // ── Step tree mutations ────────────────────────────────────────────────────

    fun addStep(step: Step) {
        _steps.add(step)
    }

    fun addStepAfter(afterId: StepId, step: Step): Boolean =
        StepTreeEditor.insertAfter(_steps, afterId, step)

    fun addStepToParent(parentId: StepId, step: Step): Boolean =
        StepTreeEditor.addToChildren(_steps, parentId, step)

    fun addStepToBranch(branchId: BranchId, step: Step): Boolean =
        StepTreeEditor.addToBranch(_steps, branchId, step)

    fun removeStep(id: StepId): Boolean =
        StepTreeEditor.remove(_steps, id)

    fun updateStep(updated: Step): Boolean =
        StepTreeEditor.replace(_steps, updated)

    fun moveStep(stepId: StepId, targetParentId: StepId?, targetIndex: Int): Boolean {
        val step = StepTreeEditor.find(_steps, stepId) ?: return false
        if (!StepTreeEditor.remove(_steps, stepId)) return false
        return StepTreeEditor.insertAt(_steps, step, targetParentId, targetIndex)
    }

    fun findStep(id: StepId): Step? = StepTreeEditor.find(_steps, id)

    fun findBranch(id: BranchId): Branch? = StepTreeEditor.findBranch(_steps, id)

    companion object {
        fun create(
            name: String,
            description: String = "",
            settings: ScriptSettings = ScriptSettings()
        ): Script {
            require(name.isNotBlank()) { "Script name must not be blank" }
            return Script(ScriptId(), name, description, emptyList(), settings)
        }

        /** Used by serialization to restore a Script from persisted data. */
        fun restore(
            id: ScriptId,
            name: String,
            description: String,
            steps: List<Step>,
            settings: ScriptSettings
        ): Script = Script(id, name, description, steps, settings)
    }
}

@kotlinx.serialization.Serializable
data class ScriptSettings(
    val defaultDelayBefore: Long = 0,
    val defaultDelayAfter: Long = 0,
    val observerCheckDelay: Long = 1000,
    val defaultImageMatchThreshold: Double = 0.7
)

