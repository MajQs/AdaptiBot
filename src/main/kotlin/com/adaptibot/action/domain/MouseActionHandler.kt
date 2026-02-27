package com.adaptibot.action.domain

import com.adaptibot.action.ActionExecutionException
import com.adaptibot.action.adapter.MouseController
import com.adaptibot.model.Action.Mouse
import com.adaptibot.vision.VisionFacade
import com.adaptibot.vision.dto.ElementLookupResult

internal class MouseActionHandler(
    private val visionFacade: VisionFacade,
) : ActionHandler<Mouse> {

    override fun handle(action: Mouse) {
        when (action) {
            is Mouse.Click -> {
                action.target
                    ?.let { visionFacade.findElement(it).toCoordinateOrThrow() }
                    ?.let { MouseController.moveTo(it) }
                MouseController.click(action.button, action.type, action.holdDuration)
            }

            is Mouse.Drag -> {
                val toCoordinate = visionFacade.findElement(action.to).toCoordinateOrThrow()
                val fromCoordinate = action.from
                    ?.let { visionFacade.findElement(it).toCoordinateOrThrow() }
                MouseController.drag(fromCoordinate, toCoordinate)
            }

            is Mouse.MoveTo -> {
                val coordinate = visionFacade.findElement(action.target).toCoordinateOrThrow()
                MouseController.moveTo(coordinate)
            }

            is Mouse.Scroll -> MouseController.scroll(action.amount, action.direction)
        }
    }
}

private fun ElementLookupResult.toCoordinateOrThrow() = when (this) {
    is ElementLookupResult.Found -> coordinate
    is ElementLookupResult.ImageNotFound -> throw ActionExecutionException.ImageNotFound(bestConfidence, threshold)
    is ElementLookupResult.CoordinateOutOfBounds -> throw ActionExecutionException.CoordinateOutOfBounds(
        given.x, given.y, screenWidth, screenHeight
    )
}

