package com.adaptibot.common.util

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

object FileUtils {
    
    fun ensureDirectoryExists(path: Path): Boolean {
        return try {
            if (!path.exists()) {
                Files.createDirectories(path)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun getAppDataDirectory(): Path {
        val userHome = System.getProperty("user.home")
        return Paths.get(userHome, ".adaptibot")
    }
    
    fun getConfigFilePath(): Path {
        return getAppDataDirectory().resolve("config.json")
    }
    
    fun getScriptsDirectory(): Path {
        return getAppDataDirectory().resolve("scripts")
    }
}

