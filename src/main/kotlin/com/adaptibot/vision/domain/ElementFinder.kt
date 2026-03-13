package com.adaptibot.vision.domain

import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.model.ImagePattern
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.vision.dto.ElementLookupResult
import org.slf4j.LoggerFactory

internal class ElementFinder(
    private val screenCapture: ScreenCapture,
    private val imageMatcher: ImageMatcher,
) {

    private val logger = LoggerFactory.getLogger(ElementFinder::class.java)

    fun find(identifier: ImagePattern): ElementLookupResult {
        return findByImage(identifier)
    }

    private fun findByImage(identifier: ImagePattern): ElementLookupResult {
        return try {
            val screenshot = screenCapture.captureFullScreen()
            val template = ImageEncoder.decodeFromBase64(identifier.base64Data)
            val threshold = identifier.matchThreshold

            when (val attempt = imageMatcher.findMatch(screenshot, template, threshold)) {
                is MatchAttemptResult.Found -> {
                    val match = attempt.matchResult
                    logger.debug(
                        "Element found at (${match.coordinate.x}, ${match.coordinate.y}) " +
                                "with confidence ${match.confidence}"
                    )
                    ElementLookupResult.Found(coordinate = match.coordinate, confidence = match.confidence)
                }

                is MatchAttemptResult.NotFound -> {
                    logger.debug(
                        "Element not found. Best confidence: ${attempt.bestConfidence}, threshold: $threshold"
                    )
                    ElementLookupResult.ImageNotFound(
                        bestConfidence = attempt.bestConfidence,
                        threshold = threshold
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error finding element by image", e)
            ElementLookupResult.ImageNotFound(bestConfidence = 0.0, threshold = identifier.matchThreshold)
        }
    }
}