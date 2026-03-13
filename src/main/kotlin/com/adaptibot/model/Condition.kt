package com.adaptibot.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Condition {
    @Serializable
    data class ElementExists(val matcher: VisualMatcher) : Condition()

    @Serializable
    data class And(val conditions: List<Condition>) : Condition()
    
    @Serializable
    data class Or(val conditions: List<Condition>) : Condition()
    
    @Serializable
    data class Not(val condition: Condition) : Condition()
}
