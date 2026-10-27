# 03. Kotlin 으로 Pulumi 사용하기

## Kotlin 은 어떤 런타임으로 실행될까?

Pulumi 의 공식 언어 목록에 "Kotlin" 이 따로 있지는 않습니다. 대신 **Pulumi Java SDK 가 JVM 바이트코드 수준에서 동작**하므로
Kotlin 코드에서 Java SDK 를 그대로 사용할 수 있습니다. `Pulumi.yaml` 의 `runtime` 도 `java` 입니다.

| 선택지 | 설명 | 이 프로젝트 |
|---|---|---|
| **공식 Java SDK + Kotlin** | `com.pulumi:pulumi`, `com.pulumi:aws` 를 Kotlin 에서 사용. 공식 지원, 최신 프로바이더 즉시 사용 가능 | 채택 |
| 커뮤니티 Kotlin SDK (VirtusLab `pulumi-kotlin`) | Kotlin 타입 안전 DSL 제공. 공식 지원 아님 | 미채택 |

## Pulumi CLI 가 Gradle 프로젝트를 실행하는 방식

`runtime: java` 인 프로젝트에서 `pulumi up` 을 실행하면 Java 언어 호스트가 다음 순서로 실행기를 고릅니다.

1. 현재 디렉터리에 `build.gradle(.kts)` / `settings.gradle(.kts)` 가 있으면 **Gradle 프로젝트**로 인식
2. 상위 디렉터리로 올라가며 `settings.gradle.kts` 를 찾아 **Gradle 루트**를 결정
3. 지나온 디렉터리 이름으로 **서브프로젝트 경로**를 계산 (`labs/01-s3-bucket` → `:labs:01-s3-bucket`)
4. 루트의 `./gradlew` (없으면 `gradle`)로 `:labs:01-s3-bucket:run` 실행

그래서 이 저장소는 **하나의 Gradle 멀티 프로젝트 안에 여러 Pulumi 프로젝트**를 둘 수 있습니다.

```
hello-pulumi/
├── settings.gradle.kts            ← (2) Gradle 루트
├── gradlew                        ← (4) 실행 도구
└── labs/01-s3-bucket/
    ├── Pulumi.yaml                ← pulumi 명령은 여기서 실행
    └── build.gradle.kts           ← (1) Gradle 프로젝트로 인식
```

> Pulumi 는 사용할 프로바이더 플러그인을 알아내기 위해 `-PmainClass=com.pulumi.bootstrap.internal.Main --args=packages` 로
> 한 번 더 `run` 을 호출합니다. 그래서 `application.mainClass` 는 **`mainClass` Gradle 프로퍼티를 우선**하도록 설정해야 합니다.
> 이 처리는 컨벤션 플러그인에 들어 있습니다.

## 빌드 구성

### 버전 카탈로그 — `gradle/libs.versions.toml`

```toml
[versions]
kotlin = "2.4.21"
pulumi = "1.37.8"
pulumi-aws = "7.52.0"
```

### 컨벤션 플러그인 — `buildSrc/src/main/kotlin/pulumi-kotlin-lab.gradle.kts`

모든 lab 이 공유하는 빌드 규칙입니다.

```kotlin
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("org.jetbrains.kotlinx.kover")
}

kotlin { jvmToolchain(21) }

dependencies {
    implementation(lib("pulumi-sdk"))
    implementation(lib("pulumi-aws"))
    // ... 테스트 의존성
}

application {
    // Pulumi CLI 가 넘기는 -PmainClass 를 우선 사용, 없으면 lab 의 MainKt
    mainClass.set(
        providers.gradleProperty("mainClass")
            .orElse(provider { "${project.property("labPackage")}.MainKt" })
    )
}
```

### lab 빌드 스크립트 — 단 3줄

```kotlin
// labs/01-s3-bucket/build.gradle.kts
plugins { id("pulumi-kotlin-lab") }
extra["labPackage"] = "io.hellopulumi.lab01"
```

새 lab 을 추가할 때는 디렉터리를 만들고 `settings.gradle.kts` 의 `include(...)` 에 한 줄 추가하면 됩니다.

## 프로그램 구조 컨벤션

테스트하기 쉽도록 **진입점(Main.kt)과 인프라 정의(XxxStack.kt)를 분리**합니다.

```kotlin
// Main.kt — Pulumi 엔진과 연결되는 얇은 진입점 (설정 읽기만 담당)
fun main() {
    Pulumi.run { ctx ->
        val config = ctx.config()
        val settings = NetworkSettings.from { key -> config.get(key).orElse(null) }
        VpcEc2Stack.define(ctx, settings)
    }
}

// VpcEc2Stack.kt — 실제 인프라 선언 (테스트 대상)
object VpcEc2Stack {
    fun define(ctx: Context, settings: NetworkSettings) { ... }
}
```

- 설정은 **검증된 불변 data class** 로 변환해 넘깁니다 (시스템 경계에서 한 번에 검증).
- 테스트에서는 `define()` 을 Mock 환경에서 직접 호출합니다 ([11. 테스트](11-testing.md)).

## Kotlin ↔ Java SDK 상호운용 팁

### 1. 빌더 패턴

Java SDK 의 모든 Args 는 빌더입니다. 각 setter 는 **일반 값과 `Output<T>` 두 가지 오버로드**를 가지므로 Output 을 그대로 넘기면 됩니다.

```kotlin
BucketVersioningArgs.builder()
    .bucket(bucket.id())   // Output<String> 오버로드
    .versioningConfiguration(
        BucketVersioningVersioningConfigurationArgs.builder().status("Enabled").build()
    )
    .build()
```

### 2. `Optional` 로 감싸진 Output

선택(optional) 속성의 getter 는 `Output<Optional<T>>` 를 반환합니다.

```kotlin
bucket.tags()                // Output<Optional<Map<String, String>>>
bucket.arn()                 // Output<String>  (항상 존재)
```

### 3. `Either` 타입 입력

AWS provider v7 에서 정책 문서처럼 "문자열 또는 구조체"를 받는 입력은 `Either` 입니다.
Kotlin 은 Java 처럼 오버로드를 추론하지 못하므로 타입을 명시해 감싸 줍니다.

```kotlin
.policy(bucket.arn().applyValue { arn ->
    Either.ofLeft<String, PolicyDocumentArgs>(publicReadPolicy(arn))
})
```

### 4. 와일드카드 Output (`Output<?>`)

`StackReference.requireOutput()` 은 `Output<?>` 를 반환합니다. Kotlin 확장 함수로 타입을 좁혀 사용합니다.

```kotlin
@Suppress("UNCHECKED_CAST")
fun StackReference.requireString(name: String): Output<String> =
    (requireOutput(name) as Output<Any?>).applyValue { it as? String ?: error("'$name' 은 문자열이 아닙니다") }
```

### 5. 람다와 SAM 변환

`Pulumi.run` 은 `Consumer<Context>` 를 받으므로 Kotlin 람다나 함수 참조를 그대로 넘길 수 있습니다.

```kotlin
Pulumi.run { ctx -> S3BucketStack.define(ctx) }
Pulumi.run(::defineSites)  // fun defineSites(ctx: Context)
```

### 6. JSON 문서 작성

IAM/버킷 정책은 Kotlin raw string 으로 작성하면 읽기 좋습니다. 더 복잡해지면 Gson `JsonObject` 나
`PolicyDocumentArgs` 빌더(타입 안전)를 고려하세요.

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [02. 핵심 개념](02-core-concepts.md) | [목차](../README.md#문서-학습-순서) | [04. 실습 환경 준비](04-setup.md) |
