package com.adaptibot.vision

import com.adaptibot.script.value.ElementLocation
import com.adaptibot.script.value.ImagePattern

sealed class VisionQuery {

    abstract val location: ElementLocation

    data class ByImage(
        val pattern: ImagePattern,
        override val location: ElementLocation = ElementLocation.Anywhere,
    ) : VisionQuery()

    data class ByText(
        val text: String,
        override val location: ElementLocation = ElementLocation.Anywhere,
    ) : VisionQuery()
}

