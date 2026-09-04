package com.adaptibot.infrastructure

import com.adaptibot.script.value.ScreenRect
import java.awt.image.BufferedImage

/**
 * Makes all screen captures taken inside one logical "tick" share their results.
 *
 * The observer thread evaluates every armed observer in a single pass; without sharing, two
 * observers that watch the same area each pay for their own screenshot, which is the dominant
 * cost of a check (a full virtual-desktop grab is tens of megabytes).
 *
 * Captures are cached **per requested region**, not merged into one full-screen frame: a small
 * pinned region is still grabbed directly, so an observer with a narrow search area never pays
 * for a full-screen capture it does not need.
 *
 * The cache is thread-confined and lives only for the duration of [shareCaptures], so a captured
 * frame is never reused across ticks and the screen state seen by one pass stays consistent.
 */
object SharedScreenFrame {

    private val currentFrame = ThreadLocal<MutableMap<ScreenRect, BufferedImage>?>()

    /**
     * Runs [block] with capture sharing enabled. Nested calls join the enclosing frame instead of
     * starting a new one.
     */
    fun <T> shareCaptures(block: () -> T): T {
        if (currentFrame.get() != null) return block()

        currentFrame.set(HashMap())
        try {
            return block()
        } finally {
            currentFrame.remove()
        }
    }

    /**
     * Returns the capture of [region] from the current frame, taking it with [capture] on first
     * request. Outside [shareCaptures] the capture is always taken directly.
     */
    internal fun capture(region: ScreenRect, capture: (ScreenRect) -> BufferedImage): BufferedImage {
        val frame = currentFrame.get() ?: return capture(region)
        return frame.getOrPut(region) { capture(region) }
    }
}

