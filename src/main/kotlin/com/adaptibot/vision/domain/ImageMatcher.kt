package com.adaptibot.vision.domain

import com.adaptibot.model.Coordinate
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

internal class ImageMatcher {

    private val logger = LoggerFactory.getLogger(ImageMatcher::class.java)

    init {
        try {
            OpenCV.loadLocally()
            logger.info("OpenCV loaded successfully")
        } catch (e: Exception) {
            logger.error("Failed to load OpenCV", e)
            throw ImageMatcherException("Failed to initialize OpenCV: ${e.message}", e)
        }
    }

    fun findBestMatch(
        screenshot: BufferedImage,
        template: BufferedImage
    ): MatchResult? {
        try {
            val screenshotMat = bufferedImageToMat(screenshot)
            val templateMat = bufferedImageToMat(template)

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
            Imgproc.matchTemplate(screenshotMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)

            val mmr = Core.minMaxLoc(result)
            val matchValue = mmr.maxVal
            val topLeft = mmr.maxLoc
            val centerX = (topLeft.x + templateCols / 2).toInt()
            val centerY = (topLeft.y + templateRows / 2).toInt()

            screenshotMat.release()
            templateMat.release()
            result.release()

            logger.debug("Best image match: confidence=$matchValue, position=($centerX, $centerY)")

            return MatchResult(
                coordinate = Coordinate(centerX, centerY),
                confidence = matchValue,
                topLeft = Coordinate(topLeft.x.toInt(), topLeft.y.toInt()),
                bottomRight = Coordinate(
                    (topLeft.x + templateCols).toInt(),
                    (topLeft.y + templateRows).toInt()
                )
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

data class MatchResult(
    val coordinate: Coordinate,
    val confidence: Double,
    val topLeft: Coordinate,
    val bottomRight: Coordinate,
)

class ImageMatcherException(message: String, cause: Throwable? = null) : Exception(message, cause)
