package com.adaptibot.action.domain

import com.adaptibot.action.ActionExecutionException.CoordinateOutOfBounds
import com.adaptibot.action.ActionExecutionException.ImageNotFound
import com.adaptibot.action.ActionExecutionException.TextNotFound
import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.Target
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.VisionQuery

internal class TargetCoordinateResolver(
    private val visionFacade: VisionFacade
) {

    fun resolve(target: Target): Coordinate = when (target) {
        is Target.AtCoordinate -> atCoordinate(target)
        is Target.AtImage      -> atImage(target)
        is Target.AtText       -> atText(target)
    }

    private fun atCoordinate(target: Target.AtCoordinate): Coordinate {
        val bounds = ScreenCapture.virtualBounds()
        val c = target.coordinate
        if (!bounds.contains(c)) {
            throw CoordinateOutOfBounds(c.x, c.y, bounds)
        }
        return c
    }

    private fun atImage(target: Target.AtImage): Coordinate {
        val match = visionFacade.find(VisionQuery.ByImage(target.pattern, target.location))
        if (match == null || match.confidence < target.pattern.matchThreshold) {
            throw ImageNotFound(match?.confidence ?: 0.0, target.pattern.matchThreshold)
        }
        return match.coordinate
    }

    private fun atText(target: Target.AtText): Coordinate {
        val match = visionFacade.find(VisionQuery.ByText(target.text, target.location))
            ?: throw TextNotFound(target.text)
        return match.coordinate
    }
}
