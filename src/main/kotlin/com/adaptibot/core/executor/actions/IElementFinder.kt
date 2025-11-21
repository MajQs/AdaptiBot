package com.adaptibot.core.executor.actions

import com.adaptibot.common.model.Coordinate
import com.adaptibot.common.model.ElementIdentifier

interface IElementFinder {
    fun find(identifier: ElementIdentifier): Coordinate?
}

