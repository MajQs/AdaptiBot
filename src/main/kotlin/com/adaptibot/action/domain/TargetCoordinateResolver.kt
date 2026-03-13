package com.adaptibot.action.domain

import com.adaptibot.action.ActionExecutionException.CoordinateOutOfBounds
import com.adaptibot.action.ActionExecutionException.ImageNotFound
import com.adaptibot.model.Coordinate
import com.adaptibot.model.Target
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.dto.ElementLookupResult
import java.awt.Toolkit

internal class TargetCoordinateResolver(
    private val visionFacade: VisionFacade
) {

    fun resolve(target: Target): Coordinate = when (target) {
        is Target.AtCoordinate -> atCoordinate(target)
        is Target.AtImage -> atImage(target)
    }

    private fun atCoordinate(target: Target.AtCoordinate): Coordinate {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val c = target.coordinate
        if (c.x < 0 || c.y < 0 || c.x > screenSize.width || c.y > screenSize.height) {
            throw CoordinateOutOfBounds(c.x, c.y, screenSize.width, screenSize.height)
        }
        return c
    }

    private fun atImage(target: Target.AtImage): Coordinate =
        visionFacade.findImage(target.pattern)
            .let { lookupResult ->
                when (lookupResult) {
                    is ElementLookupResult.Found -> lookupResult.coordinate
                    is ElementLookupResult.ImageNotFound -> throw ImageNotFound(
                        lookupResult.bestConfidence,
                        lookupResult.threshold
                    )
                }
            }
}

