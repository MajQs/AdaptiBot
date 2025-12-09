package com.adaptibot.vision.match

import com.adaptibot.common.model.Coordinate
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class ImageMatcher {
    
    private val logger = LoggerFactory.getLogger(ImageMatcher::class.java)
    
    init {
        try {
            nu.pattern.OpenCV.loadLocally()
            logger.info("OpenCV loaded successfully")
        } catch (e: Exception) {
            logger.error("Failed to load OpenCV", e)
            throw ImageMatcherException("Failed to initialize OpenCV: ${e.message}", e)
        }
    }
    
    fun findMatch(
        screenshot: BufferedImage,
        template: BufferedImage,
        threshold: Double = 0.7
    ): MatchResult? {
        try {
            val screenshotMat = bufferedImageToMat(screenshot)
            val templateMat = bufferedImageToMat(template)
            
            val resultCols = screenshotMat.cols() - templateMat.cols() + 1
            val resultRows = screenshotMat.rows() - templateMat.rows() + 1
            
            if (resultCols <= 0 || resultRows <= 0) {
                logger.warn("Template is larger than screenshot")
                return null
            }
            
            val result = Mat(resultRows, resultCols, CvType.CV_32FC1)
            
            Imgproc.matchTemplate(screenshotMat, templateMat, result, Imgproc.TM_CCOEFF_NORMED)
            
            val mmr = Core.minMaxLoc(result)
            val matchValue = mmr.maxVal
            
            screenshotMat.release()
            templateMat.release()
            result.release()
            
            if (matchValue >= threshold) {
                val topLeft = mmr.maxLoc
                val centerX = (topLeft.x + templateMat.cols() / 2).toInt()
                val centerY = (topLeft.y + templateMat.rows() / 2).toInt()
                
                logger.debug("Image match found: confidence=${matchValue}, position=($centerX, $centerY)")
                
                return MatchResult(
                    coordinate = Coordinate(centerX, centerY),
                    confidence = matchValue,
                    topLeft = Coordinate(topLeft.x.toInt(), topLeft.y.toInt()),
                    bottomRight = Coordinate(
                        (topLeft.x + templateMat.cols()).toInt(),
                        (topLeft.y + templateMat.rows()).toInt()
                    )
                )
            } else {
                logger.debug("No match found above threshold. Best match: $matchValue (threshold: $threshold)")
                return null
            }
            
        } catch (e: Exception) {
            logger.error("Error during image matching", e)
            throw ImageMatcherException("Image matching failed: ${e.message}", e)
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
    val bottomRight: Coordinate
)

class ImageMatcherException(message: String, cause: Throwable? = null) : Exception(message, cause)


