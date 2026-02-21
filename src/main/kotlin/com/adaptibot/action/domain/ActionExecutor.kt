package com.adaptibot.action.domain

import com.adaptibot.action.adapter.KeyboardController
import com.adaptibot.action.adapter.MouseController
import com.adaptibot.common.model.Action
import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier
import com.adaptibot.engine.domain.actions.ElementFinder
import org.slf4j.LoggerFactory

internal class ActionExecutor(
    private val elementFinder: ElementFinder
) {

    private val logger = LoggerFactory.getLogger(ActionExecutor::class.java)

    fun execute(action: Action) {
        val resolvedCoordinate = extractTargetFromAction(action)?.let { elementFinder.find(it) }

        when (action) {
            is Action.Mouse -> executeMouse(action, resolvedCoordinate)
            is Action.Keyboard -> executeKeyboard(action)
            is Action.System -> executeSystem(action)
            is Action.Flow -> executeFlow(action)
        }
    }

    private fun extractTargetFromAction(action: Action): ElementIdentifier? {
        return when (action) {
            is Action.Mouse.LeftClick -> action.target
            is Action.Mouse.RightClick -> action.target
            is Action.Mouse.DoubleClick -> action.target
            is Action.Mouse.MoveTo -> action.target
            else -> null
        }
    }


    private fun executeMouse(action: Action.Mouse, coordinate: Coordinate?): Boolean {
        return try {
            when (action) {
                is Action.Mouse.LeftClick -> {
                    coordinate?.let { MouseController.moveTo(it) }
                    MouseController.leftClick()
                }

                is Action.Mouse.RightClick -> {
                    coordinate?.let { MouseController.moveTo(it) }
                    MouseController.rightClick()
                }

                is Action.Mouse.DoubleClick -> {
                    coordinate?.let { MouseController.moveTo(it) }
                    MouseController.doubleClick()
                }

                is Action.Mouse.MoveTo -> {
                    coordinate?.let { MouseController.moveTo(it) } ?: false
                }

                is Action.Mouse.Drag -> {
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
                        logger.error("Drag action requires both coordinates")
                        false
                    }
                }

                is Action.Mouse.Scroll -> {
                    MouseController.scroll(action.amount, action.direction)
                }
            }
        } catch (e: Exception) {
            logger.error("Mouse action failed", e)
            false
        }
    }

    private fun executeKeyboard(action: Action.Keyboard): Boolean {
        return try {
            when (action) {
                is Action.Keyboard.TypeText -> {
                    KeyboardController.typeText(action.text)
                }

                is Action.Keyboard.PressKey -> {
                    KeyboardController.pressKey(action.key)
                }

                is Action.Keyboard.PressKeyCombination -> {
                    KeyboardController.pressKeyCombination(action.keys)
                }
            }
        } catch (e: Exception) {
            logger.error("Keyboard action failed", e)
            false
        }
    }

    private fun executeSystem(action: Action.System): Boolean {
        // TODO: Implement system actions
        return when (action) {
            is Action.System.Wait -> {
                Thread.sleep(action.milliseconds)
                true
            }

            is Action.System.LaunchApplication -> {
                // TODO: Implement process launch
                true
            }

            is Action.System.CloseApplication -> {
                // TODO: Implement process termination
                true
            }
        }
    }

    private fun executeFlow(action: Action.Flow): Boolean {
        // TODO: Implement flow control (handled by executor)
        return true
    }
}