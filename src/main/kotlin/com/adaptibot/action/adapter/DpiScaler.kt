package com.adaptibot.action.adapter

import com.adaptibot.model.Coordinate
import org.slf4j.LoggerFactory
import java.awt.GraphicsEnvironment

internal object DpiScaler {

    private val logger = LoggerFactory.getLogger(DpiScaler::class.java)

    val scaleX: Double
    val scaleY: Double

    init {
        val transform = GraphicsEnvironment
            .getLocalGraphicsEnvironment()
            .defaultScreenDevice
            .defaultConfiguration
            .defaultTransform
        scaleX = transform.scaleX
        scaleY = transform.scaleY
        logger.info("DPI scale factors – X: $scaleX, Y: $scaleY")
    }

    fun toPhysical(coordinate: Coordinate): Coordinate =
        Coordinate(
            (coordinate.x * scaleX).toInt(),
            (coordinate.y * scaleY).toInt()
        )
}

