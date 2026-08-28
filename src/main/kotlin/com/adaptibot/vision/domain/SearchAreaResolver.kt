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
                ?.let { it.expand(pinMarginFor(query, it)) }
                ?.intersect(virtualBounds)
                ?: virtualBounds
        }
    }

    /** OCR needs whitespace around a word, so a tight crop finds nothing - text gets a far larger margin. */
    private fun pinMarginFor(query: VisionQuery, pinned: ScreenRect): Int = when (query) {
        is VisionQuery.ByImage -> PIN_MARGIN_PX
        is VisionQuery.ByText -> maxOf(TEXT_PIN_MIN_MARGIN_PX, pinned.height * 2)
    }

    companion object {
        const val PIN_MARGIN_PX = 8
        const val TEXT_PIN_MIN_MARGIN_PX = 40
    }
}

internal fun VisionQuery.cacheKey(): String = when (this) {
    is VisionQuery.ByImage -> "image:${pattern.base64Data.hashCode()}"
    is VisionQuery.ByText -> "text:$text"
}
