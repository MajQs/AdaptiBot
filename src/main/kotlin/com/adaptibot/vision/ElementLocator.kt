package com.adaptibot.vision

import com.adaptibot.model.ElementIdentifier

interface ElementLocator {
    fun find(identifier: ElementIdentifier): ElementLookupResult
}

