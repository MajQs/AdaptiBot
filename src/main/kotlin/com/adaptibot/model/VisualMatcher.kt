package com.adaptibot.model

import kotlinx.serialization.Serializable


@Serializable
sealed class VisualMatcher {

    @Serializable
    data class ImagePresent(val pattern: ImagePattern) : VisualMatcher()
}

