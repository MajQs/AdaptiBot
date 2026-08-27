package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.serialization.ImageEncoder
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
) {

    private val logger = LoggerFactory.getLogger(VisionFinder::class.java)

    fun find(query: VisionQuery): MatchDataDto? = VisionMetrics.measure(Phase.TOTAL) {
        val screenshot = VisionMetrics.measure(Phase.CAPTURE) { screenCapture.captureFullScreen() }

        val result = when (query) {
            is VisionQuery.ByImage -> {
                val template = VisionMetrics.measure(Phase.TEMPLATE_DECODE) {
                    ImageEncoder.decodeFromBase64(query.pattern.base64Data)
                }
                imageMatcher.match(screenshot, template)
            }

            is VisionQuery.ByText -> VisionMetrics.measure(Phase.OCR) {
                textMatcher.match(screenshot, query.text)
            }
        }

        logger.debug(
            "Vision query {} over {}x{} screenshot -> {}",
            query::class.simpleName,
            screenshot.width,
            screenshot.height,
            result?.let { "match at ${it.coordinate} (confidence=${it.confidence})" } ?: "no match"
        )

        result
    }
}

