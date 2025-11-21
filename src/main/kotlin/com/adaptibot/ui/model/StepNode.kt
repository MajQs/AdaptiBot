package com.adaptibot.ui.model

import com.adaptibot.common.model.Step

data class StepNode(
    val step: Step,
    val displayText: String,
    val icon: String = "📄",
    val isExpanded: Boolean = true
) {
    companion object {
        fun from(step: Step): StepNode {
            val (displayText, icon) = when (step) {
                is Step.ActionStep -> {
                    val actionType = step.action::class.simpleName ?: "Action"
                    val label = step.label ?: actionType
                    Pair(label, getActionIcon(step.action))
                }
                is Step.ConditionalBlock -> {
                    val label = step.label ?: "IF Condition"
                    Pair(label, "🔀")
                }
                is Step.ObserverBlock -> {
                    val label = step.label ?: "Observer"
                    Pair(label, "👁️")
                }
                is Step.GroupBlock -> {
                    Pair(step.name, "📁")
                }
            }
            
            return StepNode(step, displayText, icon)
        }
        
        private fun getActionIcon(action: com.adaptibot.common.model.Action): String {
            return when (action) {
                is com.adaptibot.common.model.Action.Mouse -> "🖱️"
                is com.adaptibot.common.model.Action.Keyboard -> "⌨️"
                is com.adaptibot.common.model.Action.System -> "⚙️"
                is com.adaptibot.common.model.Action.Flow -> "➡️"
            }
        }
    }
}

