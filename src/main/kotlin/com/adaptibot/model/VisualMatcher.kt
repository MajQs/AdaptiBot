package com.adaptibot.model

import com.adaptibot.model.Coordinate
import kotlinx.serialization.Serializable

@Serializable
sealed class VisualMatcher {

    @Serializable
    data class ImagePresent(val pattern: ImagePattern) : VisualMatcher()

    @Serializable
    data class ColorAt(
        val point: Coordinate,
        val expected: PixelColor,
        val tolerance: ColorTolerance = ColorTolerance()
    ) : VisualMatcher()
}

