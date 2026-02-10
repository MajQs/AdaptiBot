package com.adaptibot.core.dto

data class ExecutionContextDto(
    val scriptName: String,
    val state: ExecutionStateDto,
    val iterationCount: Long
)

