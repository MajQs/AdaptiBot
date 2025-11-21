package com.adaptibot.common.model

import kotlinx.serialization.Serializable

@Serializable
sealed class Condition {
    @Serializable
    data class ElementExists(val identifier: ElementIdentifier) : Condition()
    
    @Serializable
    data class ElementNotExists(val identifier: ElementIdentifier) : Condition()
    
    @Serializable
    data class And(val conditions: List<Condition>) : Condition()
    
    @Serializable
    data class Or(val conditions: List<Condition>) : Condition()
    
    @Serializable
    data class Not(val condition: Condition) : Condition()
}

