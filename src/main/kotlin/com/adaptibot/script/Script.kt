package com.adaptibot.script

import com.adaptibot.script.step.ContainerId
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepContainer
import com.adaptibot.script.step.StepDuplicator
import com.adaptibot.script.step.StepId
import com.adaptibot.script.step.StepTreeEditor

class Script private constructor(
    val id: ScriptId,
    name: String,
    description: String,
    rootContainer: StepContainer,
    val settings: ScriptSettings
) {
    var name: String = name
        private set

    var description: String = description
        private set

    private var rootContainer: StepContainer = rootContainer

    val rootContainerId: ContainerId get() = rootContainer.id

    val steps: List<Step> get() = rootContainer.steps

    internal fun getRootContainer(): StepContainer = rootContainer

    // ── Metadata ───────────────────────────────────────────────────────────────

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Script name must not be blank" }
        name = newName
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
    }

    // ── Step tree mutations ────────────────────────────────────────────────────

    fun addStepAfter(afterId: StepId, step: Step): Boolean {
        val newRoot = StepTreeEditor.insertAfter(rootContainer, afterId, step) ?: return false
        rootContainer = newRoot
        return true
    }

    fun addStep(containerId: ContainerId, step: Step, index: Int = Int.MAX_VALUE): Boolean {
        val newRoot = StepTreeEditor.insertAt(rootContainer, step, containerId, index) ?: return false
        rootContainer = newRoot
        return true
    }

    fun removeStep(id: StepId): Boolean {
        val newRoot = StepTreeEditor.remove(rootContainer, id) ?: return false
        rootContainer = newRoot
        return true
    }

    fun updateStep(updated: Step): Boolean {
        val newRoot = StepTreeEditor.replace(rootContainer, updated) ?: return false
        rootContainer = newRoot
        return true
    }

    fun findStep(id: StepId): Step? = StepTreeEditor.find(rootContainer, id)

    fun duplicateStep(id: StepId): Step? {
        val original = StepTreeEditor.find(rootContainer, id) ?: return null
        val clone = StepDuplicator.duplicate(original)
        val newRoot = StepTreeEditor.insertAfter(rootContainer, id, clone) ?: return null
        rootContainer = newRoot
        return clone
    }

    fun moveStep(stepId: StepId, targetContainerId: ContainerId, targetIndex: Int = Int.MAX_VALUE): Boolean {
        val step = StepTreeEditor.find(rootContainer, stepId) ?: return false
        val afterRemove = StepTreeEditor.remove(rootContainer, stepId) ?: return false
        val afterInsert = StepTreeEditor.insertAt(afterRemove, step, targetContainerId, targetIndex) ?: return false
        rootContainer = afterInsert
        return true
    }

    companion object {
        fun create(
            name: String,
            description: String = "",
            settings: ScriptSettings = ScriptSettings()
        ): Script {
            require(name.isNotBlank()) { "Script name must not be blank" }
            return Script(ScriptId(), name, description, StepContainer(), settings)
        }

        /** Used by serialization to restore a Script from persisted data. */
        fun restore(
            id: ScriptId,
            name: String,
            description: String,
            rootContainer: StepContainer,
            settings: ScriptSettings
        ): Script = Script(id, name, description, rootContainer, settings)
    }
}

@kotlinx.serialization.Serializable
data class ScriptSettings(
    val defaultDelayBefore: Long = 1000L,
    val observerCheckDelay: Long = 1000,
    val defaultImageMatchThreshold: Double = 0.7
)
