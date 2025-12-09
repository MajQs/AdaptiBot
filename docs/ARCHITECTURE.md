# AdaptiBot Architecture Documentation

## Overview

AdaptiBot follows a **layered architecture** with clear separation of concerns and dependency injection for testability. The application is built on Kotlin/JVM with JavaFX UI and native system integration through JNA.

## Core Principles

1. **Modularity** - Each layer has well-defined interfaces and responsibilities
2. **Testability** - Dependencies are injected, interfaces can be mocked
3. **Scalability** - Prepared for future migration to node-based visual editor
4. **Thread Safety** - Observers run in dedicated coroutine, proper synchronization
5. **Fail-Safe** - Errors are logged but don't stop script execution

## Package Structure

```
com.adaptibot/
├─ common/              # Shared models, config, utilities
│  ├─ config/           # App configuration (AppConfig, ConfigManager)
│  ├─ model/            # Data classes (Script, Step, Action, Condition)
│  └─ util/             # Helpers (Result, FileUtils, Extensions)
├─ core/                # Execution engine, business logic
│  ├─ executor/         # Main execution loop and state management
│  │  ├─ flow/          # (Reserved) Flow control logic
│  │  ├─ actions/       # Action execution and dispatching
│  │  └─ observer/      # Async observer thread management
│  ├─ script/           # (Reserved) Script parsing and lifecycle
│  └─ validation/       # Script and step validation rules
├─ ui/                  # JavaFX user interface
│  ├─ controller/       # FXML controllers, event handlers
│  ├─ view/             # Custom UI components (MainView, EditorPane, LogsPane)
│  ├─ component/        # (Reserved) Reusable UI widgets
│  ├─ debug/            # (Reserved) Debug mode UI
│  └─ service/          # UI business logic (ScriptService, ExecutionService)
├─ automation/          # System integration for input emulation
│  ├─ input/
│  │  ├─ mouse/         # Mouse control via WinAPI
│  │  └─ keyboard/      # Keyboard control via WinAPI
│  ├─ system/           # System commands (launch, close apps)
│  └─ winapi/           # JNA wrappers for native calls
├─ vision/              # Image recognition
│  ├─ capture/          # Screen capture utilities
│  ├─ match/            # Template matching algorithms (OpenCV)
│  └─ util/             # Vision-specific helpers
├─ serialization/       # Persistence layer
│  ├─ json/             # Script JSON serialization
│  └─ image/            # Image encoding (Base64)
└─ distribution/        # Build scripts and packaging metadata
```

## Key Components

### 1. Data Models (`common/model`)

**Script** - Top-level container
```kotlin
data class Script(
    val name: String,
    val description: String = "",
    val steps: List<Step>,
    val settings: ScriptSettings = ScriptSettings()
)
```

**Step** - Composite pattern for unlimited nesting
```kotlin
sealed class Step {
    data class ActionStep(...)        // Single action
    data class ConditionalBlock(...)  // IF/ELSE
    data class ObserverBlock(...)     // Event listener
    data class GroupBlock(...)        // Named group for organization
}
```

**GroupBlock** - Organizational container
- Groups related actions into named blocks (e.g., "Login Process", "Search Flow")
- Does not affect execution logic - steps execute sequentially
- Can be nested within other blocks
- Supports collapse/expand in UI for better readability
- Validated to ensure non-blank name and non-empty content
- Full support in serialization, validation, and UI

**Action** - Sealed hierarchy of all automatable actions
- `Action.Mouse.*` - LeftClick, RightClick, MoveTo, Drag, Scroll
- `Action.Keyboard.*` - TypeText, PressKey, PressKeyCombination
- `Action.System.*` - Wait, LaunchApplication, CloseApplication
- `Action.Flow.*` - Stop, JumpTo, Continue

### 2. Core Executor (`core/executor`)

**ScriptExecutor** - Main execution engine
- Infinite loop execution with coroutines
- State management (IDLE, RUNNING, PAUSED, STOPPED)
- Coordinates with ObserverManager
- Handles flow control (jumps, stops)

**ActionDispatcher** - Routes actions to appropriate handlers
- Mouse actions → MouseController (WinAPI)
- Keyboard actions → KeyboardController (WinAPI)
- System actions → ProcessManager
- Flow actions → ExecutionContext

**ElementFinder** - Locates UI elements
- By coordinates (direct)
- By image template (OpenCV matching)

**ConditionEvaluator** - Evaluates logical expressions
- ElementExists, ElementNotExists
- Logical operators: AND, OR, NOT
- Recursive evaluation

### 3. Observer System (`core/executor/observer`)

**ObserverManager** - Async event monitoring
- Runs in separate coroutine (doesn't block main executor)
- Priority-based execution: deeper nesting = higher priority
- Scope-aware: active only within parent block context
- Thread-safe with ConcurrentHashMap

**Observer Priority Algorithm:**
```
Priority = (depth * 1000) - positionInLevel
```
Example: Observer at depth 3, position 2 → Priority = 2998

### 4. UI Layer (`ui`)

**MainController** - Central coordinator
- Wires menu actions to services
- Manages toolbar button states
- Updates UI based on execution state

**Services:**
- **ScriptService** - Script CRUD operations (new, open, save)
- **ExecutionService** - Execution control (start, pause, stop, resume)

**Views:**
- **MainView** - Main application window (menu, toolbar, split pane)
- **ScriptEditorPane** - TreeView for script steps
- **LogsPane** - TableView for execution history

### 5. Validation (`core/validation`)

**ScriptValidator** - Validates entire scripts
- Unique step IDs
- Unique labels
- Valid jump targets
- Recursive validation of nested steps

**StepValidator** - Validates individual steps
- Non-negative delays
- Non-empty group names
- Recursively validates children

### 6. Serialization (`serialization`)

**ScriptSerializer** - JSON persistence
- Uses kotlinx.serialization
- Pretty-printed JSON
- File I/O with error handling

**ImageEncoder** - Image handling
- BufferedImage ↔ Base64 conversion
- Validation of encoded images
- Support for PNG, JPG, BMP

## Execution Flow

### Normal Execution

```
1. User clicks "Start"
2. ExecutionService.start(script)
3. ScriptExecutor launches coroutine
4. Infinite loop begins:
   a. For each step in script.steps:
      - executeStep(step)
      - Handle delays (before/after)
      - Check if paused/stopped
   b. Increment iteration counter
   c. Repeat from step a
5. User clicks "Stop" → executor cancels coroutine
```

### Observer Execution

```
1. ObserverManager runs in separate coroutine
2. Every 1s (configurable), checks all active observers:
   - Sort by priority (highest first)
   - Evaluate condition
   - If true: trigger observer actions
3. When observer triggers:
   a. Main executor pauses (safe synchronization point)
   b. Observer actions execute
   c. Main executor resumes (unless observer jumped)
```

### Conditional Execution

```
ConditionalBlock execution:
1. Evaluate condition (via ConditionEvaluator)
2. If TRUE:
   - Execute thenSteps sequentially
3. If FALSE:
   - Execute elseSteps (if present)
4. Continue to next step
```

## Thread Model

### Main Thread (JavaFX Application Thread)
- UI rendering and event handling
- User interactions (menu clicks, button presses)

### Executor Thread (Coroutine - Dispatchers.Default)
- Script execution loop
- Step-by-step action processing
- Coordinated pauses when observer triggers

### Observer Thread (Coroutine - Dispatchers.Default)
- Independent from main executor
- Periodic condition checking
- Signals main executor when condition met

### Thread Synchronization
- Atomic state changes (ExecutionState)
- Coroutine cancellation for clean shutdown
- ConcurrentHashMap for observer registry

## Testing Strategy

### Unit Tests
- Data models: serialization/deserialization
- Validators: all validation rules
- Utilities: Result, FileUtils, Extensions

### Integration Tests
- ScriptExecutor: end-to-end script execution
- ObserverManager: priority and scope rules
- SerDe: save → load → compare

### UI Tests (TestFX)
- Button actions trigger correct services
- Menu items work correctly
- State updates reflect in UI

## Future Extensions

### Planned Enhancements
1. **Vision Layer** - OpenCV template matching implementation
2. **Automation Layer** - Full WinAPI/JNA integration
3. **Capture Tools** - Coordinate picker, image selection dialog
4. **Node-Based Editor** - Visual flow editor (post-MVP)
5. **Plugin System** - Custom action extensibility

### Migration Path to Visual Editor
Current list-based editor can be replaced with node graph while keeping:
- Core executor unchanged (works with Step tree)
- Same data models (Script, Step hierarchy)
- Same serialization format (JSON)

Only UI layer needs replacement - architecture supports this.

## Performance Targets (PRD §6.2)

- Observer check cycle: <100ms for all active observers
- Image matching: <500ms (target <200ms) on Full HD
- RAM usage: <512 MB during normal operation
- CPU (idle): <1%
- CPU (execution): <15% (excluding intensive vision operations)

## Configuration

### Application Config (~/.adaptibot/config.json)
```json
{
  "defaultScriptSettings": {
    "delayBefore": 0,
    "delayAfter": 0,
    "observerCheckDelay": 1000,
    "imageMatchThreshold": 0.7
  },
  "ui": {
    "windowWidth": 1200.0,
    "windowHeight": 800.0,
    "editorLogsSplitRatio": 0.6
  },
  "logging": {
    "maxLogEntries": 1000,
    "logToFile": true
  }
}
```

### Script Settings (per-script override)
Each script can override global defaults in its `settings` field.

## Error Handling Philosophy

**Fail-Safe Execution:**
- Errors in individual steps are logged but **don't stop the script**
- User can review errors in logs pane
- Observer failures don't crash the main executor
- Script continues to next iteration even after errors

**Critical Errors (Stop Execution):**
- User explicitly clicks Stop button
- OutOfMemoryError or system-level failures
- Script validation fails before start

## Logging Policy

Minimal logging following .cursorrules:
- Application lifecycle (startup, shutdown)
- Critical errors and exceptions
- State transitions (script start/stop/pause)
- Observer triggers (when condition met)

NO excessive logging in hot paths (image matching loops, step execution).

