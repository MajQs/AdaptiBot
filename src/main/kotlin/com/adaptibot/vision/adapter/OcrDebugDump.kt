package com.adaptibot.vision.adapter

import org.slf4j.Logger
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

/** Dumps the exact bitmap handed to OCR, so a failed recognition can be inspected instead of guessed at. */
internal object OcrDebugDump {

    private val timestampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")

    private val directory: Path = Paths.get("ocr-debug")

    fun save(logger: Logger, image: BufferedImage, label: String) {
        if (!logger.isDebugEnabled) return

        try {
            Files.createDirectories(directory)
            val file = directory.resolve("${LocalDateTime.now().format(timestampFormat)}-$label.png")
            ImageIO.write(image, "PNG", file.toFile())
            logger.debug("OCR debug image written to {}", file.toAbsolutePath())
        } catch (e: Exception) {
            logger.debug("Could not write OCR debug image", e)
        }
    }
}

