package com.adaptibot.engine.dto

data class ExecutionContextDto(
    val scriptName: String,
    val state: ExecutionStateDto,
    val iterationCount: Long
)

