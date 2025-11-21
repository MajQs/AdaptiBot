# AdaptiBot – Tech Stack (MVP)

| Layer / Concern | Technology | Purpose |
|-----------------|------------|---------|
| Core Language & Runtime | **Kotlin 1.9\*** on **JVM 21 LTS** | Single language for all modules; modern syntax, coroutines; runs on bundled Java 21 |
| Build / Dependency | **Gradle Kotlin DSL** | Compilation, testing, packaging, jlink/jpackage tasks |
| UI Layer | **JavaFX 21** | Native-style desktop UI for Windows / Jar-portable builds |
|  | **ControlsFX** | Rich, ready-made controls beyond standard JavaFX set |
| Automation Layer (Input Emulation) | **JNA / jna-platform** | Access to WinAPI for mouse & keyboard events without native code |
|  | **JNI stubs (optional)** | High-performance native calls where latency-critical |
| Vision Layer | **OpenCV 4.x** (opencv-java) | Template-matching & basic image processing for element recognition |
|  | **BoofCV (optional)** | Utility wrappers / fallback algorithms |
| Data / Serialization | **kotlinx.serialization (JSON)** | Persist scripts and embedded Base64 screenshots |
| Concurrency | **Kotlin Coroutines** | Clean multi-thread execution (observer thread, main executor) |
| Logging & Diagnostics | **SLF4J + Logback** | Structured runtime logging; configurable levels |
|  | **JavaFX Log Pane** | Live in-app log/history UI (max 1000 entries) |
| Testing | **JUnit 5**, **TestFX**, **MockK** | Unit, UI, and mocking support with >80 % coverage goal |
| Packaging & Distribution | **jpackage / jlink** | Produce self-contained Windows .exe with embedded JVM; minimal runtime image |
|  | **Fat .jar** | Cross-platform distribution for Linux/macOS (requires Java 21+) |
| CI/CD | **GitHub Actions** | Automated build, test, and artifact creation for Win/Linux/macOS |
| Documentation | **MkDocs Material** + **Dokka** | End-user & developer docs bundled offline |

\*Exact versions frozen at project start; update via Dependabot.

> This stack fulfills all MVP requirements (performance, portability, vision, multi-threading, packaging) while keeping the toolchain Kotlin-centric and widely supported.
