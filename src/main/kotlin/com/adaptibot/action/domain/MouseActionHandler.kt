package com.adaptibot.action.domain

import com.adaptibot.action.adapter.MouseController
import com.adaptibot.common.model.Action.Mouse
import com.adaptibot.engine.domain.actions.ElementFinder
import org.slf4j.LoggerFactory

internal class MouseActionHandler(
    private val elementFinder: ElementFinder,
) : ActionHandler<Mouse> {

    private val logger = LoggerFactory.getLogger(MouseActionHandler::class.java)

    override fun handle(action: Mouse) {
        when (action) {
            is Mouse.Click -> {
                action.target
                    ?.let { elementFinder.find(it) }
                    ?.let { MouseController.moveTo(it) }
                MouseController.click(action.button, action.type, action.holdDuration)
            }

            is Mouse.Drag -> {
                val toCoordinate = elementFinder.find(action.to)
                if (toCoordinate == null) {
                    logger.error("Could not resolve 'to' target for Drag action: ${action.to}")
                    return
                }
                val fromCoordinate =  action.from
                    ?.let { elementFinder.find(it) }

                MouseController.drag(fromCoordinate, toCoordinate)
            }

            is Mouse.MoveTo -> {
                val coordinate = elementFinder.find(action.target)
                if (coordinate == null) {
                    logger.error("Could not resolve target for MoveTo action: ${action.target}")
                    return
                }
                MouseController.moveTo(coordinate)
            }

            is Mouse.Scroll -> MouseController.scroll(action.amount, action.direction)
        }
    }
}

