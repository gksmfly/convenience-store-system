plugins {
    kotlin("jvm") version "2.2.0"                          // Kotlin JVM 프로젝트
    application                                             // 실행형 애플리케이션 플러그인
    id("com.github.johnrengelman.shadow") version "8.1.1"  // 실행 가능한 단일 JAR 생성용
}

group = "com.bible"
version = "1.0.0"

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))   // Kotlin 표준 라이브러리
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(17) }            // JDK 17 버전 지정
tasks.test { useJUnitPlatform() }

application {
    mainClass.set("store.MainKt")      // 실행 진입점(Main 함수가 있는 위치)
}

// 🔽 생성되는 JAR 파일 이름을 깔끔하게 맞춤
tasks.shadowJar {
    archiveBaseName.set("convenience-store-system")
    archiveVersion.set("")             // 버전 정보 제거
    archiveClassifier.set("all")       // -all.jar 접미사 유지
}