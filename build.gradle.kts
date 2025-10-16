group = "com.bible"
version = "1.0.0"

plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

kotlin {
    // 과제 명시: JDK 24
    jvmToolchain(24)
}

tasks.test { useJUnitPlatform() }

tasks.withType<JavaExec> {
    standardInput = System.`in`
}

application {
    mainClass.set("store.AppKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.shadowJar {
    archiveBaseName.set("convenience-store-system")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("all")
}