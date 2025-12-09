package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.serialization.image.ImageEncoder
import com.adaptibot.vision.capture.ScreenCapture
import com.adaptibot.vision.match.ImageMatcher
import org.slf4j.LoggerFactory

class ElementFinderImpl : IElementFinder {
    
    private val logger = LoggerFactory.getLogger(ElementFinderImpl::class.java)
    private val imageMatcher = ImageMatcher()
    
    override fun find(identifier: ElementIdentifier): Coordinate? {
        return when (identifier) {
            is ElementIdentifier.ByCoordinate -> {
                identifier.coordinate
            }
            is ElementIdentifier.ByImage -> {
                findByImage(identifier)
            }
        }
    }
    
    private fun findByImage(identifier: ElementIdentifier.ByImage): Coordinate? {
        return try {
            val screenshot = ScreenCapture.captureFullScreen()
            val template = ImageEncoder.decodeFromBase64(identifier.pattern.base64Data)
            val threshold = identifier.pattern.matchThreshold
            
            val matchResult = imageMatcher.findMatch(screenshot, template, threshold)
            
            if (matchResult != null) {
                logger.debug("Element found at (${matchResult.coordinate.x}, ${matchResult.coordinate.y}) with confidence ${matchResult.confidence}")
                matchResult.coordinate
            } else {
                logger.debug("Element not found with threshold $threshold")
                null
            }
        } catch (e: Exception) {
            logger.error("Error finding element by image", e)
            null
        }
    }
}

