package com.adaptibot.infrastructure

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resolves runtime directories both when the application is started from sources
 * (Gradle `run`) and when it runs as a portable jpackage app-image.
 */
object AppPaths {

    /**
     * Directory containing `AdaptiBot.exe` (portable build) or the project directory
     * when started from sources.
     */
    val appDir: Path by lazy {
        val appPath = System.getProperty("jpackage.app-path")
        if (appPath != null) {
            Paths.get(appPath).toAbsolutePath().parent ?: Paths.get("").toAbsolutePath()
        } else {
            Paths.get("").toAbsolutePath()
        }
    }

    /** Directory with Tesseract language data. */
    fun tessdata(): Path = resolve("tessdata")

    /** Directory where log files are written. */
    fun logDir(): Path = appDir.resolve("logs").also { runCatching { Files.createDirectories(it) } }

    private fun resolve(name: String): Path {
        val inAppDir = appDir.resolve(name)
        return if (Files.exists(inAppDir)) inAppDir else Paths.get(name).toAbsolutePath()
    }
}

