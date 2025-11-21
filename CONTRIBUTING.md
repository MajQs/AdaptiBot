# Contributing to AdaptiBot

Thank you for your interest in contributing to AdaptiBot! This document provides guidelines and workflows for contributors.

## Code of Conduct

- Be respectful and professional
- Focus on constructive feedback
- Help maintain a welcoming environment

## Development Setup

### Prerequisites

- **JDK 21** or later
- **Git**
- IDE with Kotlin support (IntelliJ IDEA recommended)

### Getting Started

```bash
# Clone repository
git clone https://github.com/<your-org>/AdaptiBot.git
cd AdaptiBot

# Build project
./gradlew clean build

# Run application
./gradlew run

# Run tests
./gradlew test
```

## Coding Standards

### Language & Communication

All code, comments, logs, and UI text **must be in English**:

```kotlin
// ✅ GOOD
fun createNewScript() {
    logger.info("Creating new script")
}

// ❌ BAD
fun createNewScript() {
    logger.info("Tworzenie nowego skryptu")
}
```

### Logging Policy

**Minimize logging** - only add logs where truly necessary:

**Essential logs:**
- Application lifecycle events (startup, shutdown)
- Critical errors and exceptions
- State transitions in core executor
- Observer triggers (when conditions are met)

**Avoid excessive logging:**
- No debug logs for every method entry/exit
- No info logs for trivial operations
- No logs in hot paths (e.g., image matching loops)

```kotlin
// ✅ GOOD - Essential lifecycle event
class ScriptExecutor {
    fun start(script: Script) {
        logger.info("Starting script execution: ${script.name}")
        // ...
    }
}

// ❌ BAD - Excessive logging in hot path
fun executeStep(step: Step) {
    logger.debug("Entering executeStep") // DON'T DO THIS
    logger.debug("Step ID: ${step.id}")  // DON'T DO THIS
    // ...
}
```

### Documentation Policy

**Minimal KDoc/JavaDoc** - only for complex classes that require explanation:

**Document when necessary:**
- Public APIs and interfaces
- Complex algorithms (e.g., Observer priority logic, image matching)
- Non-obvious architectural decisions

**Skip documentation for:**
- Self-explanatory classes (DTOs, simple models)
- Trivial getters/setters
- Standard JavaFX controllers with obvious purpose
- Utility functions with clear names

```kotlin
// ✅ GOOD - Complex interface needs documentation
/**
 * Manages observer lifecycle and priority-based execution.
 * Runs in separate thread to check conditions asynchronously.
 */
interface IObserverManager {
    fun registerObserver(observer: Step.ObserverBlock, priority: Int)
}

// ❌ BAD - Self-explanatory data class doesn't need docs
/**
 * Represents a coordinate with X and Y values.
 */
data class Coordinate(val x: Int, val y: Int) // DON'T DO THIS
```

### Code Style

**Kotlin idiomatic style:**
- Use data classes for DTOs
- Prefer sealed classes for type hierarchies
- Use extension functions where appropriate
- Prefer immutability (`val` over `var`)
- Use meaningful names that explain intent
- Keep functions small and focused

```kotlin
// ✅ GOOD
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errorList: List<ValidationError>) : ValidationResult()
}

// ✅ GOOD
fun Long.toReadableDuration(): String {
    return when {
        this < 1000 -> "${this}ms"
        this < 60000 -> String.format("%.2fs", this / 1000.0)
        else -> String.format("%.2fm", this / 60000.0)
    }
}
```

## Git Workflow

### Branch Naming

- `feature/short-description` - New features
- `fix/short-description` - Bug fixes
- `refactor/short-description` - Code refactoring
- `docs/short-description` - Documentation changes

### Commit Messages

Follow conventional commits format:

```
type(scope): short description

Longer explanation if needed
```

Types:
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code refactoring
- `docs` - Documentation changes
- `test` - Adding/updating tests
- `chore` - Build/config changes

Examples:
```
feat(executor): add pause/resume functionality
fix(validation): handle empty label validation correctly
refactor(ui): extract ScriptService from MainController
docs(architecture): update thread model documentation
```

### Pull Request Process

1. **Create feature branch** from `main`
2. **Implement changes** following coding standards
3. **Write/update tests** for new functionality
4. **Run tests locally** - ensure all pass
5. **Update documentation** if needed
6. **Create Pull Request** with clear description
7. **Address review feedback** promptly

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing performed

## Checklist
- [ ] Code follows project style guidelines
- [ ] All tests pass locally
- [ ] Documentation updated (if needed)
- [ ] No excessive logging added
- [ ] All text in English
```

## Project Structure

See [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) for detailed architecture documentation.

Quick reference:
```
src/main/kotlin/com/adaptibot/
├─ common/          # Shared models, config, utilities
├─ core/            # Execution engine
├─ ui/              # JavaFX interface
├─ automation/      # System integration (WinAPI)
├─ vision/          # Image recognition
└─ serialization/   # Persistence
```

## Testing Guidelines

### Writing Tests

```kotlin
// Test naming: backticks with descriptive name
@Test
fun `valid script passes validation`() {
    val script = TestUtils.createTestScript()
    val result = ScriptValidator.validate(script)
    
    assertTrue(result.isValid)
    assertTrue(result.errors.isEmpty())
}
```

### Test Coverage Goals

- Core executor: >80%
- Observer logic: >90%
- Validators: 100%
- Key scenarios: 100% integration coverage

### Running Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests ScriptValidatorTest

# With coverage report
./gradlew test jacocoTestReport
```

## Building & Packaging

### Development Build

```bash
./gradlew clean build
```

### Create Distributables

```bash
# Windows .exe (self-contained with JVM)
./gradlew packageExe

# Cross-platform .jar
./gradlew shadowJar
```

## Reporting Issues

### Bug Reports

Include:
- OS and version
- Java version (if using .jar)
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs
- Screenshots (if UI issue)

### Feature Requests

Include:
- Clear use case description
- Why existing features don't solve it
- Proposed solution (optional)
- Willingness to contribute (optional)

## Questions?

- Check [docs/ARCHITECTURE.md](./docs/ARCHITECTURE.md) for design details
- Review [PRD](./.ai/prd.md) for requirements
- Open a discussion issue

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

