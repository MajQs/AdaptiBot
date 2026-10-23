package com.adaptibot.action.domain

import com.adaptibot.script.value.Action.Mouse

internal class MouseActionHandler(
    private val targetCoordinateResolver: TargetCoordinateResolver,
    private val inputController: InputController,
) : ActionHandler<Mouse> {

    override fun handle(action: Mouse) {
        when (action) {
            is Mouse.Click -> {
                action.target
                    ?.let { targetCoordinateResolver.resolve(it) }
                    ?.let(inputController::moveMouseTo)
                inputController.clickMouse(action.button, action.type, action.holdDuration)
            }

            is Mouse.Drag -> {
                val toCoordinate   = targetCoordinateResolver.resolve(action.to)
                val fromCoordinate = action.from?.let { targetCoordinateResolver.resolve(it) }
                inputController.dragMouse(fromCoordinate, toCoordinate)
            }

            is Mouse.MoveTo -> {
                val coordinate = targetCoordinateResolver.resolve(action.target)
                inputController.moveMouseTo(coordinate)
            }

            is Mouse.Scroll -> inputController.scrollMouse(action.amount, action.direction)
        }
    }
}


