package com.adaptibot.vision.domain

import com.adaptibot.vision.dto.MatchDataDto
import java.awt.image.BufferedImage

internal interface ImageMatcher {
    fun match(screenshot: BufferedImage, template: BufferedImage): MatchDataDto?
}