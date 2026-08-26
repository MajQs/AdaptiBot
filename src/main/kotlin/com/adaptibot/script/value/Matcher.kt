package com.adaptibot.script.value

import kotlinx.serialization.Serializable

@Serializable
sealed class Matcher {

    @Serializable
    data class ImagePresent(val pattern: ImagePattern) : Matcher()

    @Serializable
    data class ColorAt(
        val point: Coordinate,
        val expected: PixelColor,
        val tolerance: ColorTolerance = ColorTolerance()
    ) : Matcher()

    @Serializable
    data class TextPresent(val text: String) : Matcher()
}

