package com.adaptibot.serialization

import com.adaptibot.script.*
import com.adaptibot.script.step.*
import com.adaptibot.script.value.*
import kotlinx.serialization.Serializable

/**
 * JSON document representation of [Script].
 *
 * Stores the entire step tree as a single [rootContainer] – a [StepContainer]
 * that is the root of the script. All nested containers (GroupStep.container,
 * ConditionalStep.trueContainer, etc.) are serialized as part of the step tree.
 */
@Serializable
internal data class ScriptDocument(
    val id: ScriptId = ScriptId(),
    val name: String,
    val description: String = "",
    val rootContainer: StepContainer,
    val settings: ScriptSettings = ScriptSettings()
) {
    fun toDomain(): Script = Script.restore(
        id = id,
        name = name,
        description = description,
        rootContainer = rootContainer,
        settings = settings
    )
}

internal fun Script.toDocument(): ScriptDocument = ScriptDocument(
    id = id,
    name = name,
    description = description,
    rootContainer = rootContainer,
    settings = settings
)
