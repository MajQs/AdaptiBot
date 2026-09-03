# AdaptiBot

![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

## Table of Contents

- [Project Description](#project-description)
- [Tech Stack](#tech-stack)
- [Getting Started Locally](#getting-started-locally)
- [Available Scripts](#available-scripts)
- [Project Scope](#project-scope)
- [Project Status](#project-status)
- [License](#license)

## Project Description

AdaptiBot is a desktop application that automates complex user interactions with the graphical user interface.  
It lets you **define, save and execute automation scripts** that emulate mouse, keyboard and system operations, complete with conditional logic, action grouping and a real-time observer mechanism.

## Tech Stack

| Layer / Concern | Technology | Purpose |
|-----------------|------------|---------|
| Core Language & Runtime | Kotlin 1.9 on JVM 21 LTS | Single language for all modules; modern syntax |
| Build / Dependency | Gradle Kotlin DSL | Compilation, testing, packaging |
| UI Layer | JavaFX 21, ControlsFX | Native-style desktop UI + extra controls |
| Automation Layer | JNA 5.14 / WinAPI | Mouse & keyboard simulation via native OS calls |
| Vision Layer | OpenCV 4.7 (opencv-java) | Template matching & pixel color sampling |
| Data / Serialization | kotlinx.serialization (JSON) | Persist scripts as JSON files with embedded Base64 screenshots |
| Concurrency | Java Virtual Threads (`Thread.ofVirtual()`) | Dedicated script-execution and observer threads |
| Logging & Diagnostics | SLF4J + Logback, in-app log panel | Structured runtime logging & live execution log |
| Testing | JUnit 5, TestFX, MockK | Unit and mocking support |

## Getting Started Locally

### Prerequisites

- **JDK 21** or later (required for building and running).
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

### Portable Windows Package (for end users)

```bash
# builds build/portable/AdaptiBot-<version>-portable-win.zip
./gradlew portableZip
```

The ZIP contains a self-contained application (bundled Java runtime – **no JDK/JRE needed** on the
target machine):

```
AdaptiBot/
├── AdaptiBot.exe      <- double-click to run
├── app/               <- application and library jars
├── runtime/           <- bundled Java 21 runtime
├── tessdata/          <- OCR language data (eng, pol)
├── examples/          <- example scripts
└── logs/              <- created on first run (adaptibot.log)
```

Instructions for the recipient:

1. Unzip the archive anywhere (e.g. `C:\AdaptiBot`) – keep the whole folder together.
2. Double-click `AdaptiBot.exe`.
3. If Windows SmartScreen appears (unsigned app), choose **More info → Run anyway**.

## Available Scripts

| Command | Description |
|---------|-------------|
| `./gradlew run` | Run the application locally |
| `./gradlew test` | Execute unit tests |
| `./gradlew build` | Compile, test and package the application |
| `./gradlew portableZip` | Build the portable Windows package (unzip & run) |

## Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Shift+F12` | **Emergency stop** of the running script — works system-wide |

While a script is running the application controls the mouse, so the toolbar **Stop** button is
usually out of reach. The shortcut is registered system-wide (WinAPI `RegisterHotKey`, no
administrator rights required) and aborts the script immediately: pending waits are interrupted and
any keys or mouse buttons still held down are released.

Limitations: the shortcut does not reach windows running with a higher integrity level
(applications started as administrator) and some full-screen games with anti-cheat protection.
If another application already reserved `Ctrl+Shift+F12`, AdaptiBot logs a warning at startup and
keeps working — stopping is then only possible from the toolbar.

## Project Scope

The application delivers the following capabilities:

- **Script editor** with a tree-based view supporting unlimited steps and nesting.
- **Action grouping** – organize related actions into named `GroupBlock` blocks for better readability.
- **Mouse actions** – click (left/right/middle, single/double/hold), drag-and-drop, move to target, scroll.
- **Keyboard actions** – type text, press key combinations.
- **System actions** – wait (configurable delay), launch application, close application.
- **Dual element targeting** – by screen coordinates or by image pattern (template matching).
- **Element location declaration** – for every image pattern and every text lookup the user states where the
  element can show up: *can appear anywhere on screen*, *only within a selected area*, or *always in the
  same place*. The engine derives the searched screen region from that declaration, which is what makes
  lookups fast (a pinned element is found in ~11 ms instead of ~460 ms).
- **Multi-monitor support** – all coordinates and screen captures span the whole virtual desktop.
- **Conditional logic** (`ConditionalBlock`) with IF/ELSE branches and compound conditions using AND/OR/NOT operators.
- **Visual conditions** – detect UI elements by image presence or by pixel color at a coordinate (with configurable tolerance).
- **Observer mechanism** (`ObserverStep`) running in a dedicated background thread with scope-based lifecycle management; triggers a sequence of steps when its condition becomes true.
- **Continuous loop execution** – a script runs in an infinite loop until manually stopped.
- **Configurable script settings** – default delays before/after steps, observer check interval and image match threshold.
- **File I/O** – save and load scripts as JSON files (`kotlinx.serialization` with `kind` discriminator).
- **In-app execution log** – timestamped INFO / SUCCESS / ERROR entries with an auto-scroll log panel and a clear button.
- **Dirty-state tracking** – unsaved-change detection with a confirmation dialog when creating a new script.

Items not yet implemented (marked TODO in source): launching and terminating external processes.

## Project Status

The project is at version `0.1.0-SNAPSHOT` and is under active development.

## License

This project is licensed under the **MIT License** – see the [LICENSE](./LICENSE) file for details.