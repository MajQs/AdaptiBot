package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.script.value.ElementLocation
import com.adaptibot.script.value.ScreenRect
import com.adaptibot.vision.VisionQuery

/** Translates the user's declaration into the region that actually gets captured. */
internal class SearchAreaResolver(
    private val screenCapture: ScreenCapture,
) {

    fun resolve(query: VisionQuery): ScreenRect {
        val virtualBounds = screenCapture.virtualBounds()

        return when (val location = query.location) {
            is ElementLocation.Anywhere -> virtualBounds

            is ElementLocation.MovesWithin -> location.bounds.intersect(virtualBounds)

            is ElementLocation.Fixed -> PatternLocationCache.get(query.cacheKey(), virtualBounds)
                ?.expand(PIN_MARGIN_PX)
                ?.intersect(virtualBounds)
                ?: virtualBounds
        }
    }

    companion object {
        const val PIN_MARGIN_PX = 8
    }
}

internal fun VisionQuery.cacheKey(): String = when (this) {
    is VisionQuery.ByImage -> "image:${pattern.base64Data.hashCode()}"
    is VisionQuery.ByText -> "text:$text"
}
