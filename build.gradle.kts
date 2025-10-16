plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.bible"
version = "1.0.0"

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(24) }

application {
    // App.kt에 fun main() 이고 package store 라면 이게 맞음
    mainClass.set("store.AppKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("convenience-store-system")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("")                         // ← -all 제거
    manifest { attributes["Main-Class"] = application.mainClass.get() }
    mergeServiceFiles()
}

tasks.build { dependsOn(tasks.shadowJar) }