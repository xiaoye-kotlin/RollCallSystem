import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "com.rollCallSystem.Ye"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    implementation("org.openjfx:javafx-controls:17")
    implementation("org.openjfx:javafx-graphics:17")
    implementation("org.openjfx:javafx-media:17")  // For media functionality

    // JNA (Java Native Access) for system-level interactions
    implementation("net.java.dev.jna:jna:5.8.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")

    // Vosk Speech Recognition library
    implementation("com.alphacephei:vosk:0.3.45")

    // OkHttp for HTTP requests
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")

    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Add this to handle JavaFX + Swing integration
    implementation("org.openjfx:javafx-packager:17")

    // Optional - For Compose Desktop testing, if needed
    testImplementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        buildTypes.release {

            proguard {
//                configurationFiles.from(project.file("proguard-rules.pro"))
                isEnabled = false
            }

        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "RollCallSystem"
            packageVersion = "13.0.0"
            modules("java.instrument", "java.sql", "jdk.jfr", "jdk.unsupported", "jdk.unsupported.desktop")
            // 设置 JVM 参数，强制使用 UTF-8 编码
            jvmArgs("-Dfile.encoding=UTF-8")
        }
    }
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.swing", "javafx.media")
}