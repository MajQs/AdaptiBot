package com.adaptibot.action.domain

import com.adaptibot.action.adapter.MouseController
import com.adaptibot.script.Action.Mouse

internal class MouseActionHandler(
    private val targetCoordinateResolver: TargetCoordinateResolver,
) : ActionHandler<Mouse> {

    override fun handle(action: Mouse) {
        when (action) {
            is Mouse.Click -> {
                action.target
                    ?.let { targetCoordinateResolver.resolve(it) }
                    ?.let { MouseController.moveTo(it) }
                MouseController.click(action.button, action.type, action.holdDuration)
            }

            is Mouse.Drag -> {
                val toCoordinate   = targetCoordinateResolver.resolve(action.to)
                val fromCoordinate = action.from?.let { targetCoordinateResolver.resolve(it) }
                MouseController.drag(fromCoordinate, toCoordinate)
            }

            is Mouse.MoveTo -> {
                val coordinate = targetCoordinateResolver.resolve(action.target)
                MouseController.moveTo(coordinate)
            }

            is Mouse.Scroll -> MouseController.scroll(action.amount, action.direction)
        }
    }
}



