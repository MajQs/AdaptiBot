package com.adaptibot.vision.adapter

import com.adaptibot.script.value.Coordinate
import com.adaptibot.vision.domain.ImageMatcher
import com.adaptibot.vision.dto.MatchDataDto
import com.adaptibot.vision.metrics.VisionMetrics
import com.adaptibot.vision.metrics.VisionMetrics.Phase
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

internal class OpenCvImageMatcher : ImageMatcher {

    private val logger = LoggerFactory.getLogger(OpenCvImageMatcher::class.java)

    init {
        try {
            OpenCV.loadLocally()
            logger.info("OpenCV loaded successfully")
        } catch (e: Exception) {
            logger.error("Failed to load OpenCV", e)
            throw ImageMatcherException("Failed to initialize OpenCV: ${e.message}", e)
        }
    }

    override fun match(
        screenshot: BufferedImage,
        template: BufferedImage
    ): MatchDataDto? {
        try {
            val screenshotMat = VisionMetrics.measure(Phase.TO_MAT) { bufferedImageToMat(screenshot) }
            val templateMat = VisionMetrics.measure(Phase.TO_MAT) { bufferedImageToMat(template) }

            val templateCols = templateMat.cols()
            val templateRows = templateMat.rows()

            val resultCols = screenshotMat.cols() - templateCols + 1
            val resultRows = screenshotMat.rows() - templateRows + 1

            if (resultCols <= 0 || resultRows <= 0) {
                logger.warn("Template is larger than screenshot")
                screenshotMat.release()
                templateMat.release()
                return null
            }

            val result = Mat(resultRows, resultCols, CvType.CV_32FC1)
            VisionMetrics.measure(Phase.MATCH_TEMPLATE) {
                Imgproc.matchTemplate(screenshotMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)
            }

            val mmr = VisionMetrics.measure(Phase.MIN_MAX_LOC) { Core.minMaxLoc(result) }
            val matchValue = mmr.maxVal
            val topLeft = mmr.maxLoc
            val centerX = (topLeft.x + templateCols / 2).toInt()
            val centerY = (topLeft.y + templateRows / 2).toInt()

            screenshotMat.release()
            templateMat.release()
            result.release()

            logger.debug("Best image match: confidence=$matchValue, position=($centerX, $centerY)")

            return MatchDataDto(
                coordinate = Coordinate(centerX, centerY),
                confidence = matchValue
            )
        } catch (e: Exception) {
            logger.error("Error during image matching", e)
            return null
        }
    }

    private fun bufferedImageToMat(image: BufferedImage): Mat {
        val convertedImage = if (image.type != BufferedImage.TYPE_3BYTE_BGR) {
            val converted = BufferedImage(image.width, image.height, BufferedImage.TYPE_3BYTE_BGR)
            val g = converted.createGraphics()
            g.drawImage(image, 0, 0, null)
            g.dispose()
            converted
        } else {
            image
        }

        val pixels = (convertedImage.raster.dataBuffer as DataBufferByte).data
        val mat = Mat(convertedImage.height, convertedImage.width, CvType.CV_8UC3)
        mat.put(0, 0, pixels)

        return mat
    }
}

class ImageMatcherException(message: String, cause: Throwable? = null) : Exception(message, cause)
