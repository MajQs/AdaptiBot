package com.adaptibot.vision.domain

import com.adaptibot.script.Coordinate
import com.adaptibot.infrastructure.ScreenCapture
import com.adaptibot.script.PixelColor
import org.slf4j.LoggerFactory

internal class PixelColorReader {

    private val logger = LoggerFactory.getLogger(PixelColorReader::class.java)

    fun read(point: Coordinate): PixelColor {
        logger.debug("Reading pixel color at (${point.x}, ${point.y})")
        val pixel = ScreenCapture.getPixelColor(point.x, point.y)
        return PixelColor(
            r = pixel.red,
            g = pixel.green,
            b = pixel.blue,
            a = pixel.alpha
        )
    }
}

