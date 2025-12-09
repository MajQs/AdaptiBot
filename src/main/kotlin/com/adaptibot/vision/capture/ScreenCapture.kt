package com.adaptibot.vision.capture

import org.slf4j.LoggerFactory
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage

object ScreenCapture {
    
    private val logger = LoggerFactory.getLogger(ScreenCapture::class.java)
    private val robot = Robot()
    
    fun captureFullScreen(): BufferedImage {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenRect = Rectangle(0, 0, screenSize.width, screenSize.height)
        return robot.createScreenCapture(screenRect)
    }
    
    fun captureRegion(x: Int, y: Int, width: Int, height: Int): BufferedImage {
        require(width > 0 && height > 0) { "Width and height must be positive" }
        val rect = Rectangle(x, y, width, height)
        return robot.createScreenCapture(rect)
    }
    
    fun captureRegion(rect: Rectangle): BufferedImage {
        return robot.createScreenCapture(rect)
    }
}


