package com.adaptibot.script

import kotlinx.serialization.Serializable

@Serializable
data class PixelColor(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255
) {
    fun matches(expected: PixelColor, tolerance: ColorTolerance): Boolean =
        kotlin.math.abs(r - expected.r) <= tolerance.value &&
        kotlin.math.abs(g - expected.g) <= tolerance.value &&
        kotlin.math.abs(b - expected.b) <= tolerance.value
}

