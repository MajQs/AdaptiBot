package com.adaptibot.vision.domain

import com.adaptibot.serialization.ImageEncoder
import java.awt.image.BufferedImage
import java.util.Collections

/** Keeps decoded patterns in memory - decoding Base64+PNG on every query dominates the cost once ROI is applied. */
internal object TemplateCache {

    private const val MAX_ENTRIES = 64

    private val cache: MutableMap<String, BufferedImage> = Collections.synchronizedMap(
        object : LinkedHashMap<String, BufferedImage>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BufferedImage>): Boolean =
                size > MAX_ENTRIES
        }
    )

    fun decode(base64Data: String): BufferedImage =
        cache[base64Data] ?: ImageEncoder.decodeFromBase64(base64Data).also { cache[base64Data] = it }
}

