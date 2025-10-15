# convenience-store-system

24시간 학교 편의점 **스마트 재고 관리 시스템** (모바일 프로그래밍 과제).  
데이터 모델링(상품/재고/판매), 재고 임계치 경고, 유통기한 기반 할인, 매출·회전율 리포트 등을 **Kotlin/JVM**으로 구현했습니다.

## 핵심 기능
- 재고 추가/수정/삭제 및 조회 (카테고리·바코드·이름 기준 검색)
- 임계치(예: 30%) 미만 재고 경고 및 자동 발주 제안
- 유통기한 임박 할인 정책 (예: D-3/2/1/0 → 0/30/50/70%)
- 판매/반품 처리와 일·주간 매출 리포트, 회전율 계산
- 베스트셀러 TOP-N, 카테고리별 매출/마진 요약
- CSV 입·출력(초기 데이터 적재, 결과 내보내기)
- CLI 메뉴 기반 운영(과제 제출 요구에 맞춘 콘솔 I/O)

## 기술 스택
- **Language**: Kotlin 2.2.0
- **JDK/JVM Target**: 17+
- **Build**: Gradle Wrapper
- **Test**: kotest/junit (필요 시)
- **Packaging**: fat-jar (application 플러그인 사용 시)

## 프로젝트 구조 (요약)
```
convenience-store-system/
├─ __MACOSX/
│  ├─ convenience-store-system/
│  │  ├─ build/
│  │  │  ├─ classes/
│  │  │  ├─ distributions/
│  │  │  ├─ kotlin/
│  │  │  ├─ libs/
│  │  │  ├─ reports/
│  │  │  ├─ scripts/
│  │  │  ├─ tmp/
│  │  ├─ gradle/
│  │  │  ├─ wrapper/
│  │  ├─ src/
│  │  │  ├─ main/
│  │  │  ├─ test/
├─ convenience-store-system/
│  ├─ build/
│  │  ├─ classes/
│  │  │  ├─ kotlin/
│  │  ├─ distributions/
│  │  │  ├─ convenience-store-system-1.0.0.tar
│  │  │  ├─ convenience-store-system-1.0.0.zip
│  │  ├─ kotlin/
│  │  │  ├─ compileKotlin/
│  │  ├─ libs/
│  │  │  ├─ convenience-store-system-1.0.0-all.jar
│  │  │  ├─ convenience-store-system-1.0.0.jar
│  │  ├─ reports/
│  │  │  ├─ problems/
│  │  ├─ scripts/
│  │  │  ├─ convenience-store-system
│  │  │  ├─ convenience-store-system.bat
│  │  ├─ tmp/
│  │  │  ├─ jar/
│  │  │  ├─ shadowJar/
│  ├─ gradle/
│  │  ├─ wrapper/
│  │  │  ├─ gradle-wrapper.jar
│  │  │  ├─ gradle-wrapper.properties
│  ├─ src/
│  │  ├─ main/
│  │  │  ├─ kotlin/
│  │  │  ├─ resources/
│  │  ├─ test/
│  │  │  ├─ kotlin/
│  │  │  ├─ resources/
│  ├─ build.gradle.kts
│  ├─ gradle.properties
│  ├─ gradlew
│  ├─ gradlew.bat
│  ├─ settings.gradle.kts
```

> 주요 소스: `src/main/kotlin` 하위. 메인 엔트리: **MainKt**.

## 빠른 시작

### 1) 필수 요건
- JDK 17+ 설치
- (권장) `JAVA_HOME` 설정

### 2) 빌드
```bash
./gradlew clean build
```

빌드 산출물: `build/libs/*.jar`

### 3) 실행
**방법 A (Gradle Application 플러그인 사용 시)**  
`build.gradle.kts`에 `application { mainClass.set("MainKt") }` 가 설정되어 있다면:
```bash
./gradlew run
```

**방법 B (JAR 실행)**  
shadow/fat-jar 생성 시:
```bash
java -jar build/libs/convenience-store-system.jar
```
> fat-jar 생성이 안 된다면, application + shadow 플러그인 적용을 권장합니다.

### 4) 샘플 시나리오
1. **기초 데이터 로드**: CSV 불러오기로 품목/재고 초기화  
2. **입고**: 바코드 `8801234567890` → 수량 20 추가  
3. **판매**: `삼각김밥(참치)` 3개 판매 → 매출 반영, 재고 감소  
4. **임계치 경고**: 재고율 30% 미만 품목은 경고 리스트에 노출  
5. **유통기한 할인**: 만료 D-1 품목 자동 50% 할인 적용  
6. **리포트**: 일간 매출/회전율, TOP5 베스트셀러 출력 및 CSV 저장

## 평가 체크리스트(자가점검)
- [ ] 데이터 모델: `data class Product`, 재고/판매 엔티티 분리
- [ ] 비즈니스 로직: 재고 임계·할인·회전율·리포트 계산
- [ ] 입출력: CSV 또는 콘솔 I/O 정상 동작
- [ ] 예외 처리: 잘못된 입력(음수 수량/미존재 바코드) 방어
- [ ] 코드 품질: 함수 분리, 테스트(단위/시나리오), 주석·KDoc
- [ ] 문서: README/Review/Prompt 작성

## 라이선스
과제 제출용(학습 목적). 외부 배포 시 각종 라이브러리 라이선스 준수.
