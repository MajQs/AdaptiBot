package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.ElementLocation
import com.adaptibot.script.value.ScreenRect
import com.adaptibot.vision.VisionQuery
import com.adaptibot.vision.adapter.OpenCvImageMatcher
import com.adaptibot.vision.dto.MatchDataDto
import com.adaptibot.vision.metrics.VisionMetrics
import com.adaptibot.vision.metrics.VisionMetrics.Phase
import org.slf4j.LoggerFactory

internal class VisionFinder(
    private val screenCapture: ScreenCapture,
    private val textMatcher: TextMatcher,
    private val imageMatcher: OpenCvImageMatcher,
    private val searchAreaResolver: SearchAreaResolver,
) {

    private val logger = LoggerFactory.getLogger(VisionFinder::class.java)

    fun find(query: VisionQuery): MatchDataDto? = VisionMetrics.measure(Phase.TOTAL) {
        val region = searchAreaResolver.resolve(query)

        if (region.isEmpty) {
            logger.warn("Search area for ${query.location} is empty - nothing to search")
            return@measure null
        }

        val screenshot = VisionMetrics.measure(Phase.CAPTURE) { screenCapture.capture(region) }

        val localMatch = when (query) {
            is VisionQuery.ByImage -> {
                val template = VisionMetrics.measure(Phase.TEMPLATE_DECODE) {
                    TemplateCache.decode(query.pattern.base64Data)
                }

                if (template.width > region.width || template.height > region.height) {
                    logger.warn(
                        "Pattern (${template.width}x${template.height}) is larger than the declared " +
                                "search area ($region) - it can never be found there"
                    )
                    return@measure null
                }

                imageMatcher.match(screenshot, template)
            }

            is VisionQuery.ByText -> VisionMetrics.measure(Phase.OCR) {
                textMatcher.match(screenshot, query.text)
            }
        }

        val match = localMatch?.toAbsolute(region)

        if (match != null) rememberIfFixed(query, match)

        logger.debug(
            "Vision query {} over {} -> {}",
            query::class.simpleName,
            region,
            match?.let { "match at ${it.coordinate} (confidence=${it.confidence})" } ?: "no match"
        )

        match
    }

    fun forgetPinnedLocations() = PatternLocationCache.clear()

    private fun rememberIfFixed(query: VisionQuery, match: MatchDataDto) {
        if (query.location !is ElementLocation.Fixed) return
        if (query is VisionQuery.ByImage && match.confidence < query.pattern.matchThreshold) return

        PatternLocationCache.remember(
            key = query.cacheKey(),
            region = ScreenRect(
                x = match.coordinate.x - match.width / 2,
                y = match.coordinate.y - match.height / 2,
                width = match.width,
                height = match.height
            )
        )
    }

    private fun MatchDataDto.toAbsolute(region: ScreenRect): MatchDataDto =
        copy(coordinate = Coordinate(coordinate.x + region.x, coordinate.y + region.y))
}

