package com.adaptibot.common.model

import kotlinx.serialization.Serializable

@Serializable
data class ImagePattern(
    val base64Data: String,
    val matchThreshold: Double = 0.7
)

