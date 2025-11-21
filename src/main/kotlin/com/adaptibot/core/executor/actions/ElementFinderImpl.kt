package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier
import org.slf4j.LoggerFactory

class ElementFinderImpl : IElementFinder {
    
    private val logger = LoggerFactory.getLogger(ElementFinderImpl::class.java)
    
    override fun find(identifier: ElementIdentifier): Coordinate? {
        return when (identifier) {
            is ElementIdentifier.ByCoordinate -> {
                identifier.coordinate
            }
            is ElementIdentifier.ByImage -> {
                // TODO: Implement image matching using OpenCV
                null
            }
        }
    }
}

