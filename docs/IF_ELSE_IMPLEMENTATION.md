# IF/ELSE and Conditional Logic Implementation

## Overview

This document describes the implementation of conditional logic (IF/ELSE blocks) and image-based conditions in AdaptiBot.

## Components Implemented

### 1. OpenCV Image Matching

#### Vision Layer
- **`ScreenCapture`** (`vision/capture/ScreenCapture.kt`)
  - Captures full screen or specific regions
  - Uses Java Robot API for screenshot functionality
  
- **`ImageMatcher`** (`vision/match/ImageMatcher.kt`)
  - Template matching using OpenCV's `matchTemplate` with `TM_CCOEFF_NORMED`
  - Configurable threshold for match confidence (default: 0.7)
  - Returns match result with coordinates and confidence score
  - Converts BufferedImage to OpenCV Mat for processing

#### Element Finder
- **`ElementFinderImpl`** (`core/executor/actions/ElementFinderImpl.kt`)
  - Updated to support image-based element identification
  - Integrates with ImageMatcher for finding elements on screen
  - Handles both coordinate-based and image-based identification

### 2. Conditional Block Dialogs

#### ConditionalBlockEditorDialog
- **Purpose**: Create and edit IF/ELSE blocks
- **Features**:
  - Step ID, label, and delay configuration
  - Embedded ConditionBuilderPane for defining conditions
  - Preserves THEN and ELSE steps when editing
  
#### ObserverBlockEditorDialog
- **Purpose**: Create and edit Observer blocks
- **Features**:
  - Similar to ConditionalBlockEditorDialog
  - Specialized for Observer conditions
  - Preserves action steps when editing

### 3. Condition Builder UI

#### ConditionBuilderPane
- **Purpose**: Visual editor for building complex conditions
- **Supported Condition Types**:
  - **SIMPLE**: Single element check (exists/not exists)
  - **AND**: All sub-conditions must be true
  - **OR**: At least one sub-condition must be true
  - **NOT**: Negation of a condition
  
- **Identifier Types**:
  - **BY_COORDINATE**: Direct X/Y coordinates
  - **BY_IMAGE**: Template matching with Base64 encoded image
  
- **Features**:
  - Nested conditions support (unlimited depth)
  - Visual tree representation
  - Add/remove sub-conditions dynamically
  - Integrated screen capture tool

### 4. Screen Capture Tool

#### ScreenCaptureDialog
- **Purpose**: Capture regions of the screen for image-based conditions
- **Features**:
  - Full-screen overlay for region selection
  - Click and drag to select region
  - Live preview of captured image
  - ESC to cancel
  - Returns Base64 encoded image

#### ImageIdentifierPane
- **Purpose**: Reusable component for element identification
- **Features**:
  - Toggle between coordinate and image-based identification
  - Integrated capture button
  - Threshold configuration for image matching

### 5. UI Integration

#### MainController
- Added handlers for:
  - `handleAddConditionalBlock()` - Create new IF/ELSE blocks
  - `handleAddObserverBlock()` - Create new Observer blocks
  - Edit support for both block types
  
#### MainView
- Updated Edit menu with new options:
  - "Add IF/ELSE Block"
  - "Add Observer Block"

## Condition Evaluation

The `ConditionEvaluatorImpl` evaluates conditions recursively:

1. **ElementExists**: Returns true if element is found on screen
2. **ElementNotExists**: Returns true if element is NOT found
3. **And**: All sub-conditions must evaluate to true
4. **Or**: At least one sub-condition must evaluate to true
5. **Not**: Inverts the result of the inner condition

## Script Execution Flow

1. When a **ConditionalBlock** is encountered:
   - Condition is evaluated using `ConditionEvaluatorImpl`
   - If true: THEN steps are executed
   - If false: ELSE steps are executed (if present)
   
2. When an **ObserverBlock** is registered:
   - Condition is checked in separate thread
   - When condition becomes true: action steps are executed
   - Main script flow is interrupted and resumed after Observer actions

## Data Model

### Condition (sealed class)
```kotlin
sealed class Condition {
    data class ElementExists(val identifier: ElementIdentifier)
    data class ElementNotExists(val identifier: ElementIdentifier)
    data class And(val conditions: List<Condition>)
    data class Or(val conditions: List<Condition>)
    data class Not(val condition: Condition)
}
```

### ElementIdentifier (sealed class)
```kotlin
sealed class ElementIdentifier {
    data class ByCoordinate(val coordinate: Coordinate)
    data class ByImage(val pattern: ImagePattern)
}
```

### ImagePattern
```kotlin
data class ImagePattern(
    val base64Data: String,
    val matchThreshold: Double = 0.7
)
```

## Testing

### ConditionEvaluatorTest
- Tests all condition types (EXISTS, NOT_EXISTS, AND, OR, NOT)
- Tests nested conditions
- Uses MockK for mocking IElementFinder
- All tests passing ✓

## Requirements Fulfilled

From PRD v1.2:

### RF-042 to RF-050: Conditional Logic
- ✓ RF-042: System enables IF block creation
- ✓ RF-043: System enables ELSE branch addition
- ✓ RF-044: Unlimited nesting depth support
- ✓ RF-045: Check if element exists on screen
- ✓ RF-046: Check if element NOT exists on screen
- ✓ RF-047: AND operator support
- ✓ RF-048: OR operator support
- ✓ RF-049: NOT operator support
- ✓ RF-050: Complex logical expressions with multiple operators

### RF-035 to RF-041: Image Recognition
- ✓ RF-035: Screen region capture as template
- ✓ RF-036: Load image template from file
- ✓ RF-037: Screen search for best template match
- ✓ RF-038: Configurable match threshold
- ✓ RF-039: Template visualization in UI
- ✓ RF-040: Return coordinates of found element (center)
- ✓ RF-041: Log when element not found

### RF-117 to RF-120: Screen Capture Tool
- ✓ RF-117: Tool for marking screen area
- ✓ RF-118: Free rectangular area selection
- ✓ RF-119: Preview before confirmation
- ✓ RF-120: Automatic attachment to step

## Architecture Notes

- **Separation of concerns**: Vision layer isolated from UI and core execution
- **Testability**: ConditionEvaluator uses dependency injection for IElementFinder
- **Extensibility**: Sealed classes allow easy addition of new condition types
- **Performance**: OpenCV native library for fast template matching
- **User Experience**: Visual condition builder with drag-and-drop feel

## Next Steps (Future Enhancements)

1. Add coordinate picker tool (click on screen to get X/Y)
2. Add visual indicators during condition evaluation (debug mode)
3. Optimize image matching performance (region of interest, caching)
4. Add more identification strategies (OCR, UI Automation API)
5. Add condition validation in ScriptValidator

## Files Created/Modified

### New Files
- `src/main/kotlin/com/adaptibot/vision/capture/ScreenCapture.kt`
- `src/main/kotlin/com/adaptibot/vision/match/ImageMatcher.kt`
- `src/main/kotlin/com/adaptibot/ui/dialog/ConditionalBlockEditorDialog.kt`
- `src/main/kotlin/com/adaptibot/ui/dialog/ObserverBlockEditorDialog.kt`
- `src/main/kotlin/com/adaptibot/ui/dialog/ScreenCaptureDialog.kt`
- `src/main/kotlin/com/adaptibot/ui/dialog/ImageIdentifierPane.kt`
- `src/main/kotlin/com/adaptibot/ui/view/ConditionBuilderPane.kt`
- `src/test/kotlin/com/adaptibot/core/condition/ConditionEvaluatorTest.kt`

### Modified Files
- `src/main/kotlin/com/adaptibot/core/executor/actions/ElementFinderImpl.kt`
- `src/main/kotlin/com/adaptibot/ui/controller/MainController.kt`
- `src/main/kotlin/com/adaptibot/ui/view/MainView.kt`

## Build & Test Status

✓ Build: SUCCESS
✓ Tests: ALL PASSING (10 tests)
✓ No linter errors


