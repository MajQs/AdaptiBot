package com.adaptibot.execution.domain

import com.adaptibot.script.step.BlockStep
import com.adaptibot.script.step.Step

internal class BlockStepHandler {
    fun resolve(block: BlockStep): List<Step> = block.steps
}
