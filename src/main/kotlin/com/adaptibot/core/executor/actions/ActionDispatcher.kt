package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Action
import com.adaptibot.common.model.Coordinate
import org.slf4j.LoggerFactory

class ActionDispatcher : IActionExecutor {
    
    private val logger = LoggerFactory.getLogger(ActionDispatcher::class.java)
    
    override fun execute(action: Action, resolvedCoordinate: Coordinate?): Boolean {
        return try {
            when (action) {
                is Action.Mouse -> executeMouse(action, resolvedCoordinate)
                is Action.Keyboard -> executeKeyboard(action)
                is Action.System -> executeSystem(action)
                is Action.Flow -> executeFlow(action)
            }
        } catch (e: Exception) {
            logger.error("Action execution failed: ${action::class.simpleName}", e)
            false
        }
    }
    
    private fun executeMouse(action: Action.Mouse, coordinate: Coordinate?): Boolean {
        return try {
            when (action) {
                is Action.Mouse.LeftClick -> {
                    coordinate?.let { com.adaptibot.automation.input.mouse.MouseController.moveTo(it) }
                    com.adaptibot.automation.input.mouse.MouseController.leftClick()
                }
                is Action.Mouse.RightClick -> {
                    coordinate?.let { com.adaptibot.automation.input.mouse.MouseController.moveTo(it) }
                    com.adaptibot.automation.input.mouse.MouseController.rightClick()
                }
                is Action.Mouse.DoubleClick -> {
                    coordinate?.let { com.adaptibot.automation.input.mouse.MouseController.moveTo(it) }
                    com.adaptibot.automation.input.mouse.MouseController.doubleClick()
                }
                is Action.Mouse.MoveTo -> {
                    coordinate?.let { com.adaptibot.automation.input.mouse.MouseController.moveTo(it) } ?: false
                }
                is Action.Mouse.Drag -> {
                    // Resolve both coordinates
                    val fromCoord = when (action.from) {
                        is com.adaptibot.common.model.ElementIdentifier.ByCoordinate -> 
                            (action.from as com.adaptibot.common.model.ElementIdentifier.ByCoordinate).coordinate
                        else -> null
                    }
                    val toCoord = when (action.to) {
                        is com.adaptibot.common.model.ElementIdentifier.ByCoordinate -> 
                            (action.to as com.adaptibot.common.model.ElementIdentifier.ByCoordinate).coordinate
                        else -> null
                    }
                    
                    if (fromCoord != null && toCoord != null) {
                        com.adaptibot.automation.input.mouse.MouseController.dragTo(fromCoord, toCoord)
                    } else {
                        logger.error("Drag action requires both coordinates")
                        false
                    }
                }
                is Action.Mouse.Scroll -> {
                    com.adaptibot.automation.input.mouse.MouseController.scroll(action.amount, action.direction)
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
                    com.adaptibot.automation.input.keyboard.KeyboardController.typeText(action.text)
                }
                is Action.Keyboard.PressKey -> {
                    com.adaptibot.automation.input.keyboard.KeyboardController.pressKey(action.key)
                }
                is Action.Keyboard.PressKeyCombination -> {
                    com.adaptibot.automation.input.keyboard.KeyboardController.pressKeyCombination(action.keys)
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

