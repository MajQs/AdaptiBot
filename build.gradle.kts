import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.runtime") version "2.0.1"
}

group = "com.adaptibot"
version = "0.1.0-SNAPSHOT"

repositories {
    // Use the Maven Central mirror accepted by the development proxy.
    maven { url = uri("https://repo1.maven.org/maven2") }
    mavenCentral()
}

// JavaFX configuration
javafx {
    version = "21.0.8"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.swing")
}

dependencies {
    // Kotlin standard library
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.10.2")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    // JavaFX (configured via plugin above)
    // ControlsFX for rich controls
    implementation("org.controlsfx:controlsfx:11.2.6")
    
    // JNA for WinAPI access
    implementation("net.java.dev.jna:jna:5.19.1")
    implementation("net.java.dev.jna:jna-platform:5.19.1")
    
    // OpenCV for image recognition
    implementation("org.openpnp:opencv:4.9.0-0")

    // Tesseract OCR for text recognition
    implementation("net.sourceforge.tess4j:tess4j:5.20.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.14.4")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("io.mockk:mockk:1.14.11")
}

application {
    mainClass.set("com.adaptibot.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
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

tasks.named("jpackageImage") {
    doFirst {
        val imageDir = layout.buildDirectory.dir("jpackage").get().asFile
        repeat(5) { attempt ->
            if (!imageDir.exists()) return@doFirst
            imageDir.deleteRecursively()
            if (imageDir.exists()) {
                logger.lifecycle("Waiting for '${imageDir}' to be released (attempt ${attempt + 1})...")
                Thread.sleep(500)
            }
        }
        if (imageDir.exists()) {
            throw GradleException(
                "Cannot delete '$imageDir'. Close AdaptiBot.exe and any Explorer window " +
                    "pointing at that folder, then run the build again."
            )
        }
    }
}

val copyPortableResources = tasks.register<Copy>("copyPortableResources") {
    dependsOn(tasks.named("jpackageImage"))
    val imageDir = layout.buildDirectory.dir("jpackage/AdaptiBot")
    into(imageDir)
    from("tessdata") { into("tessdata") }
    from("src/main/resources/examples") { into("examples") }
}

val portableZip = tasks.register<Zip>("portableZip") {
    group = "distribution"
    description = "Creates a portable (unzip & run) distribution with a bundled JRE"
    dependsOn(copyPortableResources)
    archiveFileName.set("AdaptiBot-$version-portable-win.zip")
    destinationDirectory.set(layout.buildDirectory.dir("portable"))
    from(layout.buildDirectory.dir("jpackage")) {
        include("AdaptiBot/**")
    }
}
