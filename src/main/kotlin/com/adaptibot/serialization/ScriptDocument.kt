package com.adaptibot.serialization

import com.adaptibot.script.*
import com.adaptibot.script.step.*
import com.adaptibot.script.value.*
import kotlinx.serialization.Serializable

/**
 * JSON document representation of [Script].
 */
@Serializable
internal data class ScriptDocument(
    val id: ScriptId = ScriptId(),
    val name: String,
    val description: String = "",
    val steps: List<Step>,
    val settings: ScriptSettings = ScriptSettings()
) {
    fun toDomain(): Script = Script.restore(
        id = id,
        name = name,
        description = description,
        steps = steps,
        settings = settings
    )
}

internal fun Script.toDocument(): ScriptDocument = ScriptDocument(
    id = id,
    name = name,
    description = description,
    steps = steps,
    settings = settings
)

