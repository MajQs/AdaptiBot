package com.adaptibot.script.value

import kotlinx.serialization.Serializable

/**
 * How the searched element behaves on screen, as declared by the user.
 * The vision module derives the search area - and therefore the performance - from it.
 */
@Serializable
sealed class ElementLocation {

    /** Can appear anywhere: searches the whole virtual desktop. */
    @Serializable
    data object Anywhere : ElementLocation()

    /** Moves, but only within [bounds]. */
    @Serializable
    data class MovesWithin(val bounds: ScreenRect) : ElementLocation()

    /** Always in the same place: first hit is remembered, later lookups only inspect that spot. */
    @Serializable
    data object Fixed : ElementLocation()
}

