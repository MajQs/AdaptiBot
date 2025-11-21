# AdaptiBot

![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)
![Build](https://github.com/<your-org>/AdaptiBot/actions/workflows/ci.yml/badge.svg)

## Table of Contents

- [Project Description](#project-description)
- [Tech Stack](#tech-stack)
- [Getting Started Locally](#getting-started-locally)
- [Available Scripts](#available-scripts)
- [Project Scope](#project-scope)
- [Project Status](#project-status)
- [License](#license)

## Project Description

AdaptiBot is an advanced desktop application for Windows (portable `.exe`) and other operating systems (portable `.jar`) that automates complex user interactions with the graphical user interface.  
It lets you **define, save and execute automation scripts** that emulate mouse, keyboard and other UI operations, complete with conditional logic and real-time event listeners.

Main goals of the MVP:

- Provide a simple **list / table-based script editor**.
- Execute scripts in an **infinite loop** while handling errors gracefully.
- Offer a **multi-threaded Observer mechanism** that reacts to UI events without blocking the main flow.
- Package as a **self-contained Windows executable** with an embedded JVM and a cross-platform `.jar`.

For a detailed functional specification see the [Product Requirements Document](./.ai/prd.md).

## Tech Stack

| Layer / Concern | Technology | Purpose |
|-----------------|------------|---------|
| Core Language & Runtime | Kotlin 1.9 on JVM 21 LTS | Single language for all modules; modern syntax & coroutines |
| Build / Dependency | Gradle Kotlin DSL | Compilation, testing, packaging, jlink/jpackage |
| UI Layer | JavaFX 21, ControlsFX | Native-style desktop UI + extra controls |
| Automation Layer | JNA / optional JNI stubs | WinAPI access for mouse & keyboard events |
| Vision Layer | OpenCV 4.x (opencv-java) / BoofCV | Template matching & image processing |
| Data / Serialization | kotlinx.serialization (JSON) | Persist scripts & embedded Base64 screenshots |
| Concurrency | Kotlin Coroutines | Clean multi-thread execution (observer thread, main executor) |
| Logging & Diagnostics | SLF4J + Logback, JavaFX log pane | Structured runtime logging & live history (max 1000 entries) |
| Testing | JUnit 5, TestFX, MockK | Unit, UI and mocking support (≥80 % coverage goal) |
| Packaging & Distribution | jpackage / jlink, fat .jar | Produce self-contained `.exe` and cross-platform `.jar` |
| CI/CD | GitHub Actions | Automated build, test & artifact creation |
| Documentation | MkDocs Material, Dokka | End-user & developer docs bundled offline |

## Getting Started Locally

### Prerequisites

- **JDK 21** or later (only required for building – the distributed `.exe` ships with an embedded JVM).
- **Git** and **Gradle** (the repository includes the Gradle Wrapper so you do not need a global installation).

### Clone & Build

```bash
# clone
git clone https://github.com/<your-org>/AdaptiBot.git
cd AdaptiBot

# clean & build all modules
./gradlew clean build
```

### Run from Sources

```bash
# launches the application using the Gradle Application plugin
./gradlew run
```

### Package Distributables

```bash
# Windows self-contained executable in build/dist
./gradlew packageExe

# Cross-platform fat-jar in build/libs
./gradlew shadowJar
```

> CI builds the same artifacts on every push and attaches them to the workflow run.

## Available Scripts

| Command | Description |
|---------|-------------|
| `./gradlew run` | Run the application locally |
| `./gradlew test` | Execute unit & UI tests |
| `./gradlew packageExe` | Create a self-contained Windows `.exe` with embedded JVM |
| `./gradlew shadowJar` | Build a cross-platform fat `.jar` |
| `./gradlew javadoc` | Generate developer API docs (Dokka) |
| `./gradlew spotlessApply` | Auto-format Kotlin sources |

## Project Scope

The MVP delivers the following MUST-HAVE capabilities (see PRD §4.1):

- Basic **script editor** (list/table view) with unlimited steps & nesting.
- Full set of **mouse & keyboard actions** (click, type, drag, scroll …).
- Dual **element identification** strategies: coordinates & template-matching vision.
- **Conditional logic** with IF/ELSE and logical operators AND/OR/NOT.
- **Observer mechanism** running in a dedicated thread with priorities & scope rules.
- **Infinite loop execution** with robust error handling & detailed logging.
- **Portable distribution**: self-contained `.exe` for Windows and fat `.jar` for Linux/macOS.

Out-of-scope items (post-MVP) include a visual node-based editor, cloud script storage, advanced AI vision, plugin ecosystem, multi-monitor support and more (see PRD §4.2).

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
│  ┌────────────┐  ┌─────────────┐  ┌──────────────────────┐ │
│  │ MainView   │  │ Controllers │  │ Services             │ │
│  │ (JavaFX)   │→ │ (Actions)   │→ │ (Script, Execution)  │ │
│  └────────────┘  └─────────────┘  └──────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                      Core Executor                           │
│  ┌──────────────┐  ┌────────────────┐  ┌────────────────┐  │
│  │ Script       │  │ Execution      │  │ Observer       │  │
│  │ Executor     │→ │ Context        │← │ Manager        │  │
│  │ (Main Loop)  │  │ (State)        │  │ (Async Thread) │  │
│  └──────────────┘  └────────────────┘  └────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                  Automation & Vision                         │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │ Action       │  │ Element     │  │ Condition        │   │
│  │ Dispatcher   │  │ Finder      │  │ Evaluator        │   │
│  │ (Mouse/Kbd)  │  │ (Vision)    │  │ (Logic)          │   │
│  └──────────────┘  └─────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────┐
│                   System Integration                         │
│  ┌──────────────┐  ┌─────────────┐  ┌──────────────────┐   │
│  │ JNA/WinAPI   │  │ OpenCV      │  │ Serialization    │   │
│  │ (Input)      │  │ (Vision)    │  │ (JSON/Base64)    │   │
│  └──────────────┘  └─────────────┘  └──────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

See [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) for detailed design documentation.

## Project Status

- Current PRD version: **v1.2 (2025-11-21)**
- Development phase: **MVP Active Development** – Core architecture complete (18 implementation steps), automation layer in progress
- Current implementation status:
  - ✅ Complete: Data models, Core executor skeleton, UI structure, Validation, Serialization
  - 🚧 In Progress: Mouse/Keyboard automation (JNA), Vision layer (OpenCV)
  - 📋 Planned: Full executor implementation, UI dialogs, Capture tools

Track progress in the PRD changelog and [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md).

## License

This project is licensed under the **MIT License** – see the [LICENSE](./LICENSE) file for details.