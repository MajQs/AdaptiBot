package com.adaptibot.serialization.json

import com.adaptibot.common.model.Script
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ScriptSerializer {
    
    private val logger = LoggerFactory.getLogger(ScriptSerializer::class.java)
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    fun serialize(script: Script): String {
        return try {
            json.encodeToString(script)
        } catch (e: Exception) {
            logger.error("Failed to serialize script", e)
            throw SerializationException("Failed to serialize script: ${e.message}", e)
        }
    }
    
    fun deserialize(jsonString: String): Script {
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            logger.error("Failed to deserialize script", e)
            throw SerializationException("Failed to deserialize script: ${e.message}", e)
        }
    }
    
    fun saveToFile(script: Script, path: Path) {
        try {
            val jsonString = serialize(script)
            path.writeText(jsonString)
            logger.info("Script saved to ${path.toAbsolutePath()}")
        } catch (e: Exception) {
            logger.error("Failed to save script to file", e)
            throw SerializationException("Failed to save script to file: ${e.message}", e)
        }
    }
    
    fun loadFromFile(path: Path): Script {
        return try {
            val jsonString = path.readText()
            deserialize(jsonString).also {
                logger.info("Script loaded from ${path.toAbsolutePath()}")
            }
        } catch (e: Exception) {
            logger.error("Failed to load script from file", e)
            throw SerializationException("Failed to load script from file: ${e.message}", e)
        }
    }
}

class SerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

