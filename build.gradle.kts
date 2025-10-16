plugins {
    kotlin("jvm") version "2.2.0"
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

tasks.jar { enabled = false }

/* 실행 가능한 fat JAR만 교수님 파일명으로 생성 */
tasks.shadowJar {
    archiveBaseName.set("convenience-store-system")
    archiveVersion.set("1.0.0")
    archiveClassifier.set("")                         // -all 제거 → …-1.0.0.jar
    manifest { attributes["Main-Class"] = "store.AppKt" }  // ← 네 main의 FQCN
    mergeServiceFiles()
}

/* ./gradlew build 시 shadowJar를 산출하게 */
tasks.build { dependsOn(tasks.shadowJar) }