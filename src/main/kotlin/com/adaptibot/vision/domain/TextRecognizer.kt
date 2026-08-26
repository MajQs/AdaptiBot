package com.adaptibot.vision.domain

import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Paths

internal class TextRecognizer {

    private val logger = LoggerFactory.getLogger(TextRecognizer::class.java)

    private val tesseract: Tesseract = Tesseract().apply {
        setDatapath(resolveTessdataPath())
        setLanguage("pol+eng")
        setPageSegMode(3)  // fully automatic page segmentation
        setOcrEngineMode(1) // LSTM only
    }

    /**
     * Resolves the tessdata directory using the following priority:
     * 1. `TESSDATA_PREFIX` environment variable (standard Tesseract convention)
     * 2. `tessdata/` folder next to the running JAR
     * 3. `tessdata/` folder in the current working directory
     */
    private fun resolveTessdataPath(): String {
        // 1. Environment variable
        val envPrefix = System.getenv("TESSDATA_PREFIX")
        if (!envPrefix.isNullOrBlank()) {
            val dir = File(envPrefix)
            if (dir.isDirectory) {
                logger.info("Using tessdata from TESSDATA_PREFIX: {}", dir.absolutePath)
                return dir.absolutePath
            }
        }

        // 2. Next to the JAR (production layout)
        val jarLocation = runCatching {
            File(TextRecognizer::class.java.protectionDomain.codeSource.location.toURI())
                .parentFile
        }.getOrNull()
        if (jarLocation != null) {
            val next = File(jarLocation, "tessdata")
            if (next.isDirectory) {
                logger.info("Using tessdata next to JAR: {}", next.absolutePath)
                return next.absolutePath
            }
        }

        // 3. Current working directory fallback
        val cwd = Paths.get("tessdata").toAbsolutePath().toString()
        logger.info("Using tessdata from working directory: {}", cwd)
        return cwd
    }

    fun findText(screenshot: BufferedImage, text: String): MatchResult? {
        return try {
            val words = tesseract.getWords(screenshot, net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD)

            val needle = text.trim().lowercase()

            val match = words.firstOrNull { word ->
                word.text.trim().lowercase().contains(needle)
            }

            if (match == null) {
                logger.debug("Text not found on screen: \"$text\"")
                return null
            }

            val rect = match.boundingBox
            val centerX = rect.x + rect.width / 2
            val centerY = rect.y + rect.height / 2

            logger.debug("Text found: \"$text\" at ($centerX, $centerY), bounding box=$rect")

            MatchResult(
                coordinate = com.adaptibot.script.value.Coordinate(centerX, centerY),
                confidence = 1.0,
                topLeft = com.adaptibot.script.value.Coordinate(rect.x, rect.y),
                bottomRight = com.adaptibot.script.value.Coordinate(rect.x + rect.width, rect.y + rect.height)
            )
        } catch (e: TesseractException) {
            logger.error("Tesseract OCR error while searching for text \"$text\"", e)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error during text recognition for \"$text\"", e)
            null
        }
    }

    /**
     * Returns `true` if [text] appears anywhere on [screenshot].
     */
    fun isTextPresent(screenshot: BufferedImage, text: String): Boolean =
        findText(screenshot, text) != null
}

