package com.adaptibot.vision.adapter

import com.adaptibot.script.value.Coordinate
import com.adaptibot.vision.dto.MatchDataDto
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.nio.file.Paths

internal class TesseractTextRecognizer {

    private val logger = LoggerFactory.getLogger(TesseractTextRecognizer::class.java)

    private val tesseract: Tesseract = Tesseract().apply {
        setDatapath(resolveTessdataPath())
        setLanguage("pol+eng")
        setPageSegMode(3)  // fully automatic page segmentation
        setOcrEngineMode(1) // LSTM only
    }

    private fun resolveTessdataPath(): String =
        Paths.get("tessdata").toAbsolutePath().toString()

    fun findText(screenshot: BufferedImage, text: String): MatchDataDto? {
        return try {
            val words = tesseract.getWords(screenshot, ITessAPI.TessPageIteratorLevel.RIL_WORD)

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

            MatchDataDto(
                coordinate = Coordinate(centerX, centerY),
                confidence = 1.0
            )
        } catch (e: TesseractException) {
            logger.error("Tesseract OCR error while searching for text \"$text\"", e)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error during text recognition for \"$text\"", e)
            null
        }
    }
}