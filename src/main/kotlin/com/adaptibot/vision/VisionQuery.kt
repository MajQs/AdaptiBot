package com.adaptibot.vision

import com.adaptibot.script.value.ImagePattern

sealed class VisionQuery {
    data class ByImage(val pattern: ImagePattern) : VisionQuery()
    data class ByText(val text: String) : VisionQuery()
}

