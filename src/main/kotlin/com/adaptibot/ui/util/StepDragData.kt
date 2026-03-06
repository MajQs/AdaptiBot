package com.adaptibot.ui.util

import com.adaptibot.model.StepId
import javafx.scene.input.DataFormat

data class StepDragData(val stepId: String) {
    companion object {
        val DATA_FORMAT = DataFormat("application/adaptibot-step")
    }
}

