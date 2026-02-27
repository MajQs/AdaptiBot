package com.adaptibot.vision.adapter

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.image.BufferedImage

//TODO decide if it should be in vision module
object ScreenCapture {

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
}