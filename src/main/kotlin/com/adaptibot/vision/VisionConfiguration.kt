package com.adaptibot.vision

object VisionConfiguration {
    fun getElementLocator(): ElementLocator = ElementFinder()
    fun getConditionEvaluator(): ConditionEvaluator = ConditionEvaluator(ElementFinder())
}

