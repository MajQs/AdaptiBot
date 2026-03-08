package com.adaptibot.vision.domain

import com.adaptibot.model.ElementIdentifier
import com.adaptibot.serialization.ImageEncoder
import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.vision.dto.ElementLookupResult
import org.slf4j.LoggerFactory
import java.awt.Toolkit

internal class ElementFinder(
    private val screenCapture: ScreenCapture,
    private val imageMatcher: ImageMatcher,
) {

    private val logger = LoggerFactory.getLogger(ElementFinder::class.java)

    fun find(identifier: ElementIdentifier): ElementLookupResult {
        return when (identifier) {
            is ElementIdentifier.ByCoordinate -> findByCoordinate(identifier)
            is ElementIdentifier.ByImage -> findByImage(identifier)
        }
    }

    private fun findByCoordinate(identifier: ElementIdentifier.ByCoordinate): ElementLookupResult {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val coordinate = identifier.coordinate

        return if (coordinate.x < 0 || coordinate.y < 0
            || coordinate.x > screenSize.width
            || coordinate.y > screenSize.height
        ) {
            logger.debug(
                "Coordinate (${coordinate.x}, ${coordinate.y}) is out of screen bounds " +
                        "(${screenSize.width}x${screenSize.height})"
            )
            //TODO wonder if raw coordinates should be chacked here. Probably ElementFinder should operate only on ImagePattern
//            ElementLookupResult.CoordinateOutOfBounds(
//                given = coordinate,
//                screenWidth = screenSize.width,
//                screenHeight = screenSize.height
//            )
            ElementLookupResult.Found(coordinate = coordinate, confidence = 1.0)

        } else {
            ElementLookupResult.Found(coordinate = coordinate, confidence = 1.0)
        }
    }

    private fun findByImage(identifier: ElementIdentifier.ByImage): ElementLookupResult {
        return try {
            val screenshot = screenCapture.captureFullScreen()
            val template = ImageEncoder.decodeFromBase64(identifier.pattern.base64Data)
            val threshold = identifier.pattern.matchThreshold

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
            ElementLookupResult.ImageNotFound(bestConfidence = 0.0, threshold = identifier.pattern.matchThreshold)
        }
    }
}