package com.adaptibot.vision.domain

import com.adaptibot.script.value.ScreenRect
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/** Remembers where an element declared as `Fixed` was found for the first time. */
internal object PatternLocationCache {

    private val logger = LoggerFactory.getLogger(PatternLocationCache::class.java)

    private val entries = ConcurrentHashMap<String, ScreenRect>()

    @Volatile
    private var knownBounds: ScreenRect? = null

    fun get(key: String, currentBounds: ScreenRect): ScreenRect? {
        invalidateIfScreensChanged(currentBounds)
        return entries[key]
    }

    fun remember(key: String, region: ScreenRect) {
        if (entries.putIfAbsent(key, region) == null) {
            logger.debug("Pinned location for {}: {}", key, region)
        }
    }

    fun clear() = entries.clear()

    private fun invalidateIfScreensChanged(currentBounds: ScreenRect) {
        if (knownBounds == currentBounds) return

        if (entries.isNotEmpty()) {
            logger.info(
                "Screen configuration changed ({} -> {}) - dropping pinned locations",
                knownBounds,
                currentBounds
            )
            entries.clear()
        }
        knownBounds = currentBounds
    }
}

