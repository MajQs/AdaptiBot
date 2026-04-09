package com.adaptibot.script.value

import kotlinx.serialization.Serializable

@Serializable
data class ImagePattern(
    val base64Data: String,
    val matchThreshold: Double = 0.7
)

