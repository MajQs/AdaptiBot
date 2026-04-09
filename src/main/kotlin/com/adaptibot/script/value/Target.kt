package com.adaptibot.script.value

import kotlinx.serialization.Serializable

@Serializable
sealed class Target {

    @Serializable
    data class AtCoordinate(val coordinate: Coordinate) : Target()

    @Serializable
    data class AtImage(val pattern: ImagePattern) : Target()
}

