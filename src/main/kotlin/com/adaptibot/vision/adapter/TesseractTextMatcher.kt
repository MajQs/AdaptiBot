package com.adaptibot.vision.adapter

import com.adaptibot.script.value.Coordinate
import com.adaptibot.infrastructure.AppPaths
import com.adaptibot.vision.domain.TextMatcher
import com.adaptibot.vision.dto.MatchDataDto
import net.sourceforge.tess4j.ITessAPI
import net.sourceforge.tess4j.Tesseract
import net.sourceforge.tess4j.TesseractException
import net.sourceforge.tess4j.Word
import org.slf4j.LoggerFactory
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal class TesseractTextMatcher : TextMatcher {

    private val logger = LoggerFactory.getLogger(TesseractTextMatcher::class.java)

    private val tesseract: Tesseract = Tesseract().apply {
        setDatapath(resolveTessdataPath())
        setLanguage("pol+eng")
        setOcrEngineMode(1)
        setVariable("user_defined_dpi", "300")
    }

    private fun resolveTessdataPath(): String =
        AppPaths.tessdata().toString()

    @Synchronized
    override fun match(screenshot: BufferedImage, text: String): MatchDataDto? {
        return try {
            val needle = text.trim().lowercase()
            val attempts = attemptsFor(screenshot)

            var recognised: Recognition? = null

            outer@ for (scale in attempts.scales) {
                val image = if (scale > 1) screenshot.scaledBy(scale) else screenshot

                for (mode in attempts.pageSegModes) {
                    val word = findWord(image, needle, mode)
                    if (word != null) {
                        logger.debug("Text \"{}\" recognised with page segmentation mode {}, scale {}", text, mode, scale)
                        recognised = Recognition(word, scale)
                        break@outer
                    }
                }
            }

            if (recognised == null) {
                logger.debug(
                    "Text not found in {}x{} crop (scales={}, modes={}): \"{}\"",
                    screenshot.width, screenshot.height, attempts.scales, attempts.pageSegModes, text
                )
                return null
            }

            val rect = recognised.word.boundingBox
            val scale = recognised.scale
            val centerX = (rect.x + rect.width / 2) / scale
            val centerY = (rect.y + rect.height / 2) / scale

            logger.debug("Text found: \"{}\" at ({}, {}), bounding box={}", text, centerX, centerY, rect)

            MatchDataDto(
                coordinate = Coordinate(centerX, centerY),
                confidence = 1.0,
                width = rect.width / scale,
                height = rect.height / scale
            )
        } catch (e: TesseractException) {
            logger.error("Tesseract OCR error while searching for text \"$text\"", e)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error during text recognition for \"$text\"", e)
            null
        }
    }

    private fun findWord(image: BufferedImage, needle: String, pageSegMode: Int): Word? {
        tesseract.setPageSegMode(pageSegMode)
        return tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD)
            .firstOrNull { it.text.trim().lowercase().contains(needle) }
    }

    /**
     * `PSM_AUTO` at native scale reads both full screenshots and crops, so it is the only attempt
     * for most cases - extra passes would be paid on every miss, and misses are the common outcome.
     * Only very small crops get one upscaled retry.
     */
    private fun attemptsFor(image: BufferedImage): Attempts {
        val scales = if (image.height < TINY_CROP_MAX_HEIGHT) {
            listOf(1, upscaleFor(image)).distinct()
        } else {
            listOf(1)
        }
        return Attempts(scales = scales, pageSegModes = listOf(PSM_AUTO))
    }

    private fun upscaleFor(image: BufferedImage): Int {
        if (image.height >= MIN_OCR_HEIGHT) return 1
        return ((MIN_OCR_HEIGHT + image.height - 1) / image.height).coerceAtMost(MAX_OCR_SCALE)
    }

    private fun BufferedImage.scaledBy(scale: Int): BufferedImage {
        val scaled = BufferedImage(width * scale, height * scale, BufferedImage.TYPE_INT_RGB)
        val graphics = scaled.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        graphics.drawImage(this, 0, 0, scaled.width, scaled.height, null)
        graphics.dispose()
        return scaled
    }

    private data class Attempts(val scales: List<Int>, val pageSegModes: List<Int>)

    private data class Recognition(val word: Word, val scale: Int)

    private companion object {
        const val PSM_AUTO = 3
        const val TINY_CROP_MAX_HEIGHT = 60
        const val MIN_OCR_HEIGHT = 120
        const val MAX_OCR_SCALE = 4
    }
}

