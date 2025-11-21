package com.adaptibot.core.validation

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errorList: List<ValidationError>) : ValidationResult() {
        constructor(vararg errors: ValidationError) : this(errors.toList())
    }
    
    val isValid: Boolean
        get() = this is Valid
    
    val errors: List<ValidationError>
        get() = when (this) {
            is Valid -> emptyList()
            is Invalid -> this.errorList
        }
}

data class ValidationError(
    val message: String,
    val field: String? = null,
    val severity: Severity = Severity.ERROR
) {
    enum class Severity {
        WARNING,
        ERROR
    }
}

