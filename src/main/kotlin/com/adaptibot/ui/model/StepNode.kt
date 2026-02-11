package com.adaptibot.ui.model

import com.adaptibot.common.model.ActionStep
import com.adaptibot.common.model.ConditionalBlock
import com.adaptibot.common.model.GroupBlock
import com.adaptibot.common.model.ObserverStep
import com.adaptibot.common.model.Step
import com.adaptibot.common.model.StepId

enum class ContainerType {
    ROOT,
    GROUP_BLOCK,
    CONDITIONAL_THEN,
    CONDITIONAL_ELSE,
    OBSERVER_BLOCK,
    NONE
}

data class StepNode(
    val step: Step,
    val displayText: String,
    val icon: String = "📄",
    val isExpanded: Boolean = true,
    val containerType: ContainerType = ContainerType.NONE,
    val parentBlockId: StepId? = null
) {
    fun isContainer(): Boolean {
        return containerType != ContainerType.NONE
    }

    companion object {
        fun from(step: Step, containerType: ContainerType = ContainerType.NONE, parentBlockId: StepId? = null): StepNode {
            val (displayText, icon) = when (step) {
                is ActionStep -> {
                    val actionType = step.action::class.simpleName ?: "Action"
                    val label = step.label ?: actionType
                    Pair(label, getActionIcon(step.action))
                }
                is ConditionalBlock -> {
                    val label = step.label ?: "IF Condition"
                    Pair(label, "🔀")
                }
                is ObserverStep -> {
                    val label = step.label ?: "Observer"
                    Pair(label, "👁️")
                }
                is GroupBlock -> {
                    val stepCount = step.steps.size
                    val displayName = if (stepCount > 0) {
                        "${step.name} ($stepCount steps)"
                    } else {
                        "${step.name} (empty)"
                    }
                    Pair(displayName, "📦")
                }
            }

            val actualContainerType = if (containerType != ContainerType.NONE) {
                containerType
            } else {
                when (step) {
                    is GroupBlock -> ContainerType.GROUP_BLOCK
                    is ObserverStep -> ContainerType.OBSERVER_BLOCK
                    else -> ContainerType.NONE
                }
            }

            return StepNode(step, displayText, icon, true, actualContainerType, parentBlockId)
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

