package com.adaptibot.script

import kotlinx.serialization.Serializable

@Serializable
data class PixelColor(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int = 255
)

