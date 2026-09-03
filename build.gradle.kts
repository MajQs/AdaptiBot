import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.runtime") version "1.13.1"
}

group = "com.adaptibot"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

// JavaFX configuration
javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    // Kotlin standard library
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.7.3")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // JavaFX (configured via plugin above)
    // ControlsFX for rich controls
    implementation("org.controlsfx:controlsfx:11.2.0")
    
    // JNA for WinAPI access
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    
    // OpenCV for image recognition
    implementation("org.openpnp:opencv:4.7.0-0")

    // Tesseract OCR for text recognition
    implementation("net.sourceforge.tess4j:tess4j:5.11.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("io.mockk:mockk:1.13.8")
}

application {
    mainClass.set("com.adaptibot.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// ---------------------------------------------------------------------------
// Portable distribution (jlink runtime + jpackage app-image, no installer)
// ---------------------------------------------------------------------------

runtime {
    options.set(listOf("--strip-debug", "--no-header-files", "--no-man-pages", "--compress", "2"))
    modules.set(
        listOf(
            "java.base",
            "java.datatransfer",
            "java.desktop",
            "java.logging",
            "java.management",
            "java.naming",
            "java.prefs",
            "java.scripting",
            "java.sql",
            "java.xml",
            "jdk.unsupported",
            "jdk.crypto.ec"
        )
    )

    jpackage {
        imageName = "AdaptiBot"
        appVersion = project.version.toString().substringBefore("-")
        skipInstaller = true
        val iconFile = file("src/main/resources/icons/adaptibot.ico")
        if (iconFile.exists()) {
            imageOptions = listOf("--icon", iconFile.absolutePath)
        }
    }
}

val copyPortableResources by tasks.registering(Copy::class) {
    dependsOn(tasks.named("jpackageImage"))
    val imageDir = layout.buildDirectory.dir("jpackage/AdaptiBot")
    into(imageDir)
    from("tessdata") { into("tessdata") }
    from("src/main/resources/examples") { into("examples") }
}

val portableZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Creates a portable (unzip & run) distribution with a bundled JRE"
    dependsOn(copyPortableResources)
    archiveFileName.set("AdaptiBot-$version-portable-win.zip")
    destinationDirectory.set(layout.buildDirectory.dir("portable"))
    from(layout.buildDirectory.dir("jpackage")) {
        include("AdaptiBot/**")
    }
}
