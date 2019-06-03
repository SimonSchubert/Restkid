plugins {
    id("kotlin-multiplatform") version "1.3.31"
    id("kotlinx-serialization") version "1.3.31"
}

repositories {
    jcenter()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://kotlin.bintray.com/ktor")
}

val os = org.gradle.internal.os.OperatingSystem.current()!!

kotlin {
    when {
        os.isWindows -> mingwX64("libui")
        os.isMacOsX -> macosX64("libui")
        os.isLinux -> linuxX64("libui")
        else -> throw Error("Unknown host")
    }.binaries.executable {

    }
    val libuiMain by sourceSets.getting {
        dependencies {
            implementation("com.github.msink:libui:0.1.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:0.11.0")
            implementation("io.ktor:ktor-client-core-native:1.2.1")
            implementation("io.ktor:ktor-client-curl:1.2.1")
        }
    }

    macosX64 {
        binaries.all {
            linkerOpts = mutableListOf("-L/usr/local/opt/curl/lib", "-L/usr/local/opt/curl/include/curl", "-lcurl")
        }
    }
}