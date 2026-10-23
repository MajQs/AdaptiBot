package com.adaptibot.infrastructure

/** Immutable platform features selected once by the application composition root. */
internal data class PlatformCapabilities(
    val editorSupported: Boolean,
    val actionSupported: Boolean,
    val globalHotkeySupported: Boolean,
    val recordingSupported: Boolean,
)

/** Centralizes host detection so domain and module code can depend only on capabilities. */
internal object PlatformCapabilityDetector {

    const val PLATFORM_OVERRIDE_PROPERTY: String = "adaptibot.platform"

    /**
     * Detects capabilities from `os.name` unless [PLATFORM_OVERRIDE_PROPERTY] is set.
     *
     * The override accepts `windows`, `macos`, `linux`, or `unsupported` and is intended for
     * deterministic composition tests. Unknown values fail fast rather than silently
     * changing which native adapters are eligible for construction.
     */
    fun detect(): PlatformCapabilities {
        val override = System.getProperty(PLATFORM_OVERRIDE_PROPERTY)
        val platform = if (override == null) {
            detectHostPlatform(System.getProperty("os.name"))
        } else {
            parseOverride(override)
        }

        return when (platform) {
            Platform.WINDOWS -> WINDOWS_CAPABILITIES
            Platform.MACOS, Platform.LINUX -> DESKTOP_ACTION_CAPABILITIES
            Platform.UNSUPPORTED -> UNSUPPORTED_CAPABILITIES
        }
    }

    private fun detectHostPlatform(osName: String): Platform = when {
        osName.startsWith("Windows", ignoreCase = true) -> Platform.WINDOWS
        osName.startsWith("Mac", ignoreCase = true) || osName.startsWith("Darwin", ignoreCase = true) -> Platform.MACOS
        osName.startsWith("Linux", ignoreCase = true) -> Platform.LINUX
        else -> Platform.UNSUPPORTED
    }

    private fun parseOverride(value: String): Platform = when (value.lowercase()) {
        "windows" -> Platform.WINDOWS
        "macos" -> Platform.MACOS
        "linux" -> Platform.LINUX
        "unsupported" -> Platform.UNSUPPORTED
        else -> throw IllegalArgumentException(
            "Invalid $PLATFORM_OVERRIDE_PROPERTY value '$value'. " +
                "Expected one of: windows, macos, linux, unsupported."
        )
    }

    private enum class Platform {
        WINDOWS,
        MACOS,
        LINUX,
        UNSUPPORTED
    }

    private val WINDOWS_CAPABILITIES = PlatformCapabilities(
        editorSupported = true,
        actionSupported = true,
        globalHotkeySupported = true,
        recordingSupported = true
    )

    private val DESKTOP_ACTION_CAPABILITIES = PlatformCapabilities(
        editorSupported = true,
        actionSupported = true,
        globalHotkeySupported = false,
        recordingSupported = false
    )

    private val UNSUPPORTED_CAPABILITIES = PlatformCapabilities(
        editorSupported = true,
        actionSupported = false,
        globalHotkeySupported = false,
        recordingSupported = false
    )
}
