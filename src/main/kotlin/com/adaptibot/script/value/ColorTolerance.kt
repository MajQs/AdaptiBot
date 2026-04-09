package com.adaptibot.script.value

import kotlinx.serialization.Serializable

@Serializable
data class ColorTolerance(val value: Int = 0) {
    init {
        require(value in 0..255) { "ColorTolerance value must be between 0 and 255, was: $value" }
    }
}

