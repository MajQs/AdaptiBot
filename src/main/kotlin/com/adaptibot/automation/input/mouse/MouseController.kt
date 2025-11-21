package com.adaptibot.automation.input.mouse

import com.adaptibot.automation.winapi.User32
import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ScrollDirection
import org.slf4j.LoggerFactory

object MouseController {
    
    private val logger = LoggerFactory.getLogger(MouseController::class.java)
    private val user32 = User32.INSTANCE
    
    fun moveTo(x: Int, y: Int): Boolean {
        return try {
            user32.SetCursorPos(x, y)
        } catch (e: Exception) {
            logger.error("Failed to move cursor to ($x, $y)", e)
            false
        }
    }
    
    fun moveTo(coordinate: Coordinate): Boolean {
        return moveTo(coordinate.x, coordinate.y)
    }
    
    fun getCurrentPosition(): Coordinate? {
        return try {
            val point = User32.POINT()
            if (user32.GetCursorPos(point)) {
                Coordinate(point.x, point.y)
            } else {
                null
            }
        } catch (e: Exception) {
            logger.error("Failed to get cursor position", e)
            null
        }
    }
    
    fun leftClick(): Boolean {
        return try {
            user32.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            Thread.sleep(50)
            user32.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
            true
        } catch (e: Exception) {
            logger.error("Failed to perform left click", e)
            false
        }
    }
    
    fun rightClick(): Boolean {
        return try {
            user32.mouse_event(User32.MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
            Thread.sleep(50)
            user32.mouse_event(User32.MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
            true
        } catch (e: Exception) {
            logger.error("Failed to perform right click", e)
            false
        }
    }
    
    fun doubleClick(): Boolean {
        return try {
            leftClick()
            Thread.sleep(100)
            leftClick()
            true
        } catch (e: Exception) {
            logger.error("Failed to perform double click", e)
            false
        }
    }
    
    fun dragTo(from: Coordinate, to: Coordinate): Boolean {
        return try {
            moveTo(from)
            Thread.sleep(100)
            
            user32.mouse_event(User32.MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
            Thread.sleep(50)
            
            moveTo(to)
            Thread.sleep(50)
            
            user32.mouse_event(User32.MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
            true
        } catch (e: Exception) {
            logger.error("Failed to perform drag from $from to $to", e)
            false
        }
    }
    
    fun scroll(amount: Int, direction: ScrollDirection): Boolean {
        return try {
            val wheelDelta = when (direction) {
                ScrollDirection.UP -> User32.WHEEL_DELTA * amount
                ScrollDirection.DOWN -> -User32.WHEEL_DELTA * amount
                ScrollDirection.LEFT, ScrollDirection.RIGHT -> {
                    logger.warn("Horizontal scrolling not yet implemented")
                    return false
                }
            }
            
            user32.mouse_event(User32.MOUSEEVENTF_WHEEL, 0, 0, wheelDelta, 0)
            true
        } catch (e: Exception) {
            logger.error("Failed to scroll $direction by $amount", e)
            false
        }
    }
}

