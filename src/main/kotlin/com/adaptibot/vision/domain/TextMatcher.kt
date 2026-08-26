package com.adaptibot.vision.domain

import com.adaptibot.vision.dto.MatchDataDto
import java.awt.image.BufferedImage

internal interface TextMatcher {
    fun match(screenshot: BufferedImage, text: String): MatchDataDto?
}
