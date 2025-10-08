plugins {
    kotlin("jvm") version "2.2.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // ✅ 추가
}

group = "com.bible"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}

// ✅ Gradle이 실행할 main 함수의 FQCN 지정 (패키지 + 파일명)
application {
    mainClass.set("store.MainKt")
}

// (선택) 산출물 이름 깔끔하게
// tasks.jar { archiveBaseName.set("convenience-store-system") }
// tasks.shadowJar { archiveBaseName.set("convenience-store-system") }