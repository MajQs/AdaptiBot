package com.adaptibot.serialization.image

import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

object ImageEncoder {
    
    private val logger = LoggerFactory.getLogger(ImageEncoder::class.java)
    
    fun encodeToBase64(image: BufferedImage, format: String = "PNG"): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            ImageIO.write(image, format, outputStream)
            val imageBytes = outputStream.toByteArray()
            Base64.getEncoder().encodeToString(imageBytes)
        } catch (e: Exception) {
            logger.error("Failed to encode image to Base64", e)
            throw ImageEncodingException("Failed to encode image: ${e.message}", e)
        }
    }
    
    fun decodeFromBase64(base64String: String): BufferedImage {
        return try {
            val imageBytes = Base64.getDecoder().decode(base64String)
            val inputStream = ByteArrayInputStream(imageBytes)
            ImageIO.read(inputStream) ?: throw ImageEncodingException("Failed to read image from Base64 data")
        } catch (e: IllegalArgumentException) {
            logger.error("Invalid Base64 string", e)
            throw ImageEncodingException("Invalid Base64 string: ${e.message}", e)
        } catch (e: Exception) {
            logger.error("Failed to decode image from Base64", e)
            throw ImageEncodingException("Failed to decode image: ${e.message}", e)
        }
    }
    
    fun validateBase64Image(base64String: String): Boolean {
        return try {
            decodeFromBase64(base64String)
            true
        } catch (e: Exception) {
            false
        }
    }
}

class ImageEncodingException(message: String, cause: Throwable? = null) : Exception(message, cause)

