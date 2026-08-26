package com.adaptibot.action.domain

import com.adaptibot.action.ActionExecutionException.CoordinateOutOfBounds
import com.adaptibot.action.ActionExecutionException.ImageNotFound
import com.adaptibot.action.ActionExecutionException.TextNotFound
import com.adaptibot.script.value.Coordinate
import com.adaptibot.script.value.Target
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.VisionQuery
import java.awt.Toolkit

internal class TargetCoordinateResolver(
    private val visionFacade: VisionFacade
) {

    fun resolve(target: Target): Coordinate = when (target) {
        is Target.AtCoordinate -> atCoordinate(target)
        is Target.AtImage      -> atImage(target)
        is Target.AtText       -> atText(target)
    }

    private fun atCoordinate(target: Target.AtCoordinate): Coordinate {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val c = target.coordinate
        if (c.x < 0 || c.y < 0 || c.x > screenSize.width || c.y > screenSize.height) {
            throw CoordinateOutOfBounds(c.x, c.y, screenSize.width, screenSize.height)
        }
        return c
    }

    private fun atImage(target: Target.AtImage): Coordinate {
        val match = visionFacade.find(VisionQuery.ByImage(target.pattern))
        if (match == null || match.confidence < target.pattern.matchThreshold) {
            throw ImageNotFound(match?.confidence ?: 0.0, target.pattern.matchThreshold)
        }
        return match.coordinate
    }

    private fun atText(target: Target.AtText): Coordinate {
        val match = visionFacade.find(VisionQuery.ByText(target.text))
            ?: throw TextNotFound(target.text)
        return match.coordinate
    }
}
