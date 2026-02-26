package com.adaptibot.vision

import com.adaptibot.common.model.ElementIdentifier

interface ElementLocator {
    fun find(identifier: ElementIdentifier): ElementLookupResult
}

