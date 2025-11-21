package com.adaptibot.common.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

object ConfigManager {
    
    private val logger = LoggerFactory.getLogger(ConfigManager::class.java)
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val configPath: Path = Paths.get(System.getProperty("user.home"), ".adaptibot", "config.json")
    
    private var currentConfig: AppConfig = AppConfig()
    
    fun load(): AppConfig {
        return try {
            if (configPath.exists()) {
                val jsonString = configPath.readText()
                currentConfig = json.decodeFromString(jsonString)
                logger.info("Configuration loaded from ${configPath.toAbsolutePath()}")
            } else {
                logger.info("No configuration file found, using defaults")
            }
            currentConfig
        } catch (e: Exception) {
            logger.error("Failed to load configuration, using defaults", e)
            AppConfig()
        }
    }
    
    fun save(config: AppConfig = currentConfig) {
        try {
            Files.createDirectories(configPath.parent)
            val jsonString = json.encodeToString(config)
            configPath.writeText(jsonString)
            currentConfig = config
            logger.info("Configuration saved to ${configPath.toAbsolutePath()}")
        } catch (e: Exception) {
            logger.error("Failed to save configuration", e)
        }
    }
    
    fun get(): AppConfig = currentConfig
}

