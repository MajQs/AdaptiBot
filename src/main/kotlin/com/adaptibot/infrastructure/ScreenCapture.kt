package com.adaptibot.infrastructure

import com.adaptibot.script.value.ScreenRect
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage

/**
 * Screen access over the virtual desktop (all monitors).
 * Coordinates are absolute and may be negative for monitors left of / above the primary one.
 */
object ScreenCapture {

    private val logger = LoggerFactory.getLogger(ScreenCapture::class.java)

    private val robot = Robot()

    init {
        logScreenConfiguration()
    }

    data class ScreenInfo(
        val id: String,
        val bounds: ScreenRect,
        val scaleX: Double,
        val scaleY: Double,
        val primary: Boolean,
    ) {
        val isScaled: Boolean get() = scaleX != 1.0 || scaleY != 1.0
    }

    fun screens(): List<ScreenInfo> {
        val environment = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val primaryDevice = environment.defaultScreenDevice

        return environment.screenDevices.map { device ->
            val configuration = device.defaultConfiguration
            val bounds = configuration.bounds
            val transform = configuration.defaultTransform

            ScreenInfo(
                id = device.iDstring,
                bounds = ScreenRect(bounds.x, bounds.y, bounds.width, bounds.height),
                scaleX = transform.scaleX,
                scaleY = transform.scaleY,
                primary = device.iDstring == primaryDevice.iDstring,
            )
        }
    }

    fun virtualBounds(): ScreenRect =
        screens().fold(ScreenRect.EMPTY) { accumulator, screen -> accumulator.union(screen.bounds) }

    fun captureFullScreen(): BufferedImage = capture(virtualBounds())

    fun capture(region: ScreenRect): BufferedImage {
        require(!region.isEmpty) { "Capture region must not be empty: $region" }

        return robot.createScreenCapture(
            Rectangle(region.x, region.y, region.width, region.height)
        )
    }

    fun captureRegion(x: Int, y: Int, width: Int, height: Int): BufferedImage {
        require(width > 0 && height > 0) { "Width and height must be positive" }
        return capture(ScreenRect(x, y, width, height))
    }

    fun getPixelColor(x: Int, y: Int): Color = robot.getPixelColor(x, y)

    fun logScreenConfiguration() {
        val screens = screens()
        logger.info("Detected ${screens.size} screen(s), virtual desktop bounds: ${virtualBounds()}")

        screens.forEach { screen ->
            logger.info(
                "  screen ${screen.id}${if (screen.primary) " (primary)" else ""}: " +
                        "bounds=${screen.bounds}, scale=${screen.scaleX}x${screen.scaleY}"
            )
        }

        logCaptureSpaceProbe()
    }

    /**
     * Detects whether [Robot] captures in logical or physical pixels: mouse coordinates are
     * logical, so a mismatch would offset every match.
     */
    private fun logCaptureSpaceProbe() {
        val probeSize = 64
        val bounds = virtualBounds()
        if (bounds.width < probeSize || bounds.height < probeSize) return

        try {
            val probeRect = Rectangle(bounds.x, bounds.y, probeSize, probeSize)
            val capture = robot.createScreenCapture(probeRect)
            val variantSizes = robot.createMultiResolutionScreenCapture(probeRect)
                .resolutionVariants
                .map { "${it.getWidth(null).toInt()}x${it.getHeight(null).toInt()}" }

            logger.info(
                "Capture space probe: requested ${probeSize}x$probeSize, " +
                        "got ${capture.width}x${capture.height}, resolution variants=$variantSizes"
            )

            if (capture.width != probeSize) {
                logger.warn(
                    "Robot returned an unexpected capture size - matches may be offset by the display scale factor."
                )
            }
        } catch (e: Exception) {
            logger.warn("Capture space probe failed", e)
        }
    }
}

