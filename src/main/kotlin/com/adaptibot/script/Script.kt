package com.adaptibot.script

import com.adaptibot.script.step.ContainerId
import com.adaptibot.script.step.Step
import com.adaptibot.script.step.StepContainer
import com.adaptibot.script.step.StepId
import com.adaptibot.script.step.StepTreeEditor

class Script private constructor(
    val id: ScriptId,
    name: String,
    description: String,
    val rootContainer: StepContainer,
    val settings: ScriptSettings
) {
    var name: String = name
        private set

    var description: String = description
        private set

    /** Convenience view – the top-level steps of the root container. */
    val steps: List<Step> get() = rootContainer.steps.toList()

    // ── Metadata ───────────────────────────────────────────────────────────────

    fun rename(newName: String) {
        require(newName.isNotBlank()) { "Script name must not be blank" }
        name = newName
    }

    fun updateDescription(newDescription: String) {
        description = newDescription
    }

    // ── Step tree mutations ────────────────────────────────────────────────────

    fun addStepAfter(afterId: StepId, step: Step): Boolean =
        StepTreeEditor.insertAfter(rootContainer, afterId, step)

    fun addStep(containerId: ContainerId, step: Step, index: Int = Int.MAX_VALUE): Boolean =
        StepTreeEditor.insertAt(rootContainer, step, containerId, index)

    fun removeStep(id: StepId): Boolean =
        StepTreeEditor.remove(rootContainer, id)

    fun updateStep(updated: Step): Boolean =
        StepTreeEditor.replace(rootContainer, updated)

    fun moveStep(stepId: StepId, targetContainerId: ContainerId, targetIndex: Int = Int.MAX_VALUE): Boolean {
        val step = StepTreeEditor.find(rootContainer, stepId) ?: return false
        if (!StepTreeEditor.remove(rootContainer, stepId)) return false
        return StepTreeEditor.insertAt(rootContainer, step, targetContainerId, targetIndex)
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
    val defaultDelayBefore: Long = 0,
    val defaultDelayAfter: Long = 0,
    val observerCheckDelay: Long = 1000,
    val defaultImageMatchThreshold: Double = 0.7
)
