package com.adaptibot.common.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ElementIdentifier {
    @Serializable
    data class ByCoordinate(val coordinate: Coordinate) : ElementIdentifier()
    
    @Serializable
    data class ByImage(val pattern: ImagePattern) : ElementIdentifier()
}

