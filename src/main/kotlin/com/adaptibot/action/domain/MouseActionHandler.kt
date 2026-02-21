package com.adaptibot.action.domain

import com.adaptibot.action.adapter.MouseController
import com.adaptibot.common.model.Action
import com.adaptibot.common.model.Action.Mouse
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.engine.domain.actions.ElementFinder

internal class MouseActionHandler(
    private val elementFinder: ElementFinder,
) : ActionHandler<Mouse>{

    override fun handle(action: Mouse) {
        val coordinate = extractTargetFromAction(action)?.let { elementFinder.find(it) }

        when (action) {
            is Mouse.MoveAndClick -> {
                coordinate?.let { MouseController.moveTo(it) }
                MouseController.leftClick()
            }

            is Mouse.LeftClick -> {
                MouseController.leftClick()
            }

            is Mouse.RightClick -> {
                MouseController.rightClick()
            }

            is Mouse.DoubleClick -> {
                MouseController.doubleClick()
            }

            is Mouse.MoveTo -> {
                coordinate?.let { MouseController.moveTo(it) } ?: false
            }

            is Mouse.Drag -> {
                // Resolve both coordinates
                val fromCoord = when (action.from) {
                    is ElementIdentifier.ByCoordinate ->
                        (action.from as ElementIdentifier.ByCoordinate).coordinate

                    else -> null
                }
                val toCoord = when (action.to) {
                    is ElementIdentifier.ByCoordinate ->
                        (action.to as ElementIdentifier.ByCoordinate).coordinate

                    else -> null
                }

                if (fromCoord != null && toCoord != null) {
                    MouseController.dragTo(fromCoord, toCoord)
                } else {
                    false
                }
            }

            is Mouse.Scroll -> {
                MouseController.scroll(action.amount, action.direction)
            }
        }
    }

    private fun extractTargetFromAction(action: Action): ElementIdentifier? {
        return when (action) {
            is Mouse.LeftClick -> action.target
            is Mouse.RightClick -> action.target
            is Mouse.DoubleClick -> action.target
            is Mouse.MoveTo -> action.target
            else -> null
        }
    }

}