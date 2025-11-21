# AdaptiBot Build & Distribution Guide

This guide covers building AdaptiBot from sources and creating distributable packages.

## Prerequisites

### Required

- **JDK 21** or later ([Adoptium](https://adoptium.net/), [Oracle](https://www.oracle.com/java/technologies/downloads/))
- **Git** for version control

### Optional

- **IntelliJ IDEA** (recommended IDE)
- **Gradle** (project includes Gradle Wrapper, global installation not required)

## Development Build

### Clone Repository

```bash
git clone https://github.com/<your-org>/AdaptiBot.git
cd AdaptiBot
```

### Build from Sources

```bash
# Clean previous builds
./gradlew clean

# Compile and run tests
./gradlew build

# Output: build/libs/AdaptiBot-<version>.jar
```

### Run Application

```bash
# Run directly from sources
./gradlew run

# Or run the compiled jar
java -jar build/libs/AdaptiBot-<version>.jar
```

## Distribution Packages

### Cross-Platform JAR (Linux/macOS/Windows)

Build a fat JAR with all dependencies:

```bash
./gradlew shadowJar

# Output: build/libs/AdaptiBot-<version>-all.jar
```

**Requirements for users:**
- Java 21+ installed on their system
- Command to run: `java -jar AdaptiBot-<version>-all.jar`

### Windows Executable (.exe)

**Note:** This feature requires additional Gradle configuration. Add to `build.gradle.kts`:

```kotlin
plugins {
    id("org.beryx.runtime") version "1.13.1"
}

runtime {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    
    jpackage {
        imageName = "AdaptiBot"
        installerName = "AdaptiBot"
        installerType = "exe"
        
        appVersion = project.version.toString()
        
        imageOptions.addAll(listOf(
            "--icon", "src/main/resources/icons/adaptibot.ico"
        ))
        
        installerOptions.addAll(listOf(
            "--win-dir-chooser",
            "--win-shortcut",
            "--win-menu"
        ))
    }
}
```

Then build:

```bash
# Create Windows installer
./gradlew jpackage

# Output: build/jpackage/AdaptiBot-<version>.exe
```

**Features:**
- Self-contained (embedded JVM 21)
- No Java installation required
- Windows installer with shortcuts
- ~80-120 MB size (includes JVM)

## Build Profiles

### Debug Build (Development)

Default mode - includes debug symbols and verbose logging:

```bash
./gradlew build
```

### Release Build (Production)

Optimized build with minimal logging:

```bash
./gradlew build -Prelease
```

## Testing

### Run All Tests

```bash
./gradlew test
```

### Run Specific Test Suite

```bash
# Unit tests only
./gradlew test --tests "*.core.*"

# UI tests
./gradlew test --tests "*.ui.*"

# Specific test class
./gradlew test --tests ScriptValidatorTest
```

### Generate Test Coverage Report

```bash
./gradlew test jacocoTestReport

# View report: build/reports/jacoco/test/html/index.html
```

## Troubleshooting

### "JAVA_HOME is not set"

Set JAVA_HOME environment variable:

**Windows:**
```powershell
$env:JAVA_HOME = "C:\Path\To\JDK21"
```

**Linux/macOS:**
```bash
export JAVA_HOME=/path/to/jdk21
```

### "gradlew: command not found"

Make gradlew executable:

```bash
chmod +x gradlew
```

### Out of Memory During Build

Increase Gradle memory in `gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### JPackage Fails on Windows

Ensure you have:
- **WiX Toolset** installed (for .msi/.exe installers)
- Download from: https://wixtoolset.org/

### OpenCV Native Libraries Not Found

OpenCV natives are included in dependencies. If issues persist:

```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies
```

## CI/CD

### GitHub Actions Workflow

Add `.github/workflows/build.yml`:

```yaml
name: Build

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '21'
    
    - name: Grant execute permission
      run: chmod +x gradlew
    
    - name: Build with Gradle
      run: ./gradlew build
    
    - name: Run tests
      run: ./gradlew test
    
    - name: Upload artifacts
      uses: actions/upload-artifact@v3
      with:
        name: AdaptiBot-jar
        path: build/libs/*.jar
```

## Performance Optimization

### Build Speed

**Enable Gradle daemon:**
```properties
# gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

**Use build cache:**
```bash
./gradlew build --build-cache
```

### Runtime Performance

**Production JVM flags:**
```bash
java -Xms512m -Xmx512m \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -jar AdaptiBot.jar
```

## Version Management

Update version in `build.gradle.kts`:

```kotlin
version = "0.1.0-SNAPSHOT"
```

Version scheme: `MAJOR.MINOR.PATCH[-SNAPSHOT]`

## Clean Build

Remove all build artifacts:

```bash
# Clean build directory
./gradlew clean

# Also clear Gradle cache
rm -rf ~/.gradle/caches/
./gradlew clean build --refresh-dependencies
```

## Support

For build issues:
1. Check this guide
2. Review [CONTRIBUTING.md](../CONTRIBUTING.md)
3. Search existing issues
4. Open new issue with build logs

