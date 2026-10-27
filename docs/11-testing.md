# 11. 인프라 코드 테스트

## 테스트 전략

Pulumi 프로그램도 일반 코드이므로 일반적인 테스트 피라미드를 적용할 수 있습니다.

| 종류 | 방법 | 속도 | 비용 | 이 프로젝트 |
|---|---|---|---|---|
| **단위 테스트** | Mock 으로 클라우드 호출을 대체해 프로그램 실행 | 밀리초 | 없음 | 모든 lab 에 적용 |
| 속성 테스트 (Policy) | CrossGuard 정책으로 preview 결과 검사 | 초 | 없음 | 소개만 |
| 통합 테스트 | 임시 스택에 실제 배포 → 검증 → 삭제 (Automation API) | 분 | 있음 | 소개만 |

## Mock 기반 단위 테스트의 원리

`PulumiTest` 는 Pulumi 엔진 대신 **`Mocks` 인터페이스**를 호출합니다.

```kotlin
interface Mocks {
    fun newResourceAsync(args: ResourceArgs): CompletableFuture<ResourceResult>   // 리소스 생성
    fun callAsync(args: CallArgs): CompletableFuture<Map<String, Any>>            // 데이터 소스(invoke)
}
```

이 저장소의 공용 Mock — [`test-support/.../FakeCloudMocks.kt`](../test-support/src/main/kotlin/io/hellopulumi/testsupport/FakeCloudMocks.kt):

```kotlin
class FakeCloudMocks(
    private val computed: Map<String, ComputedState> = emptyMap(),   // 타입별 "클라우드가 계산하는 값"
    private val calls: Map<String, Map<String, Any>> = emptyMap(),   // invoke 응답
) : Mocks {
    override fun newResourceAsync(args: Mocks.ResourceArgs) =
        completedFuture(ResourceResult.of(Optional.of("${args.name}-id"), args.inputs.orEmpty() + extra))
    ...
}
```

- 입력값을 그대로 상태로 돌려주고, `arn`, `publicIp` 처럼 **AWS 가 계산하는 값**만 테스트별로 주입합니다.
- 리소스 ID 는 `"<논리 이름>-id"` 이므로 리소스 간 연결을 쉽게 검증할 수 있습니다 (예: `vpcId == "lab04-vpc-id"`).
- 등록되지 않은 invoke 는 실패시켜, 테스트가 모르는 외부 호출을 숨기지 않습니다.

## 테스트 작성 예시

```kotlin
class VpcEc2StackTest {

    private val mocks = FakeCloudMocks(
        computed = mapOf("aws:ec2/instance:Instance" to { _ -> mapOf("publicIp" to "203.0.113.10") }),
        calls = mapOf("aws:ssm/getParameter:getParameter" to mapOf("value" to "ami-0123...", ...)),
    )

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()       // 테스트 간 전역 상태 초기화 (필수)

    @Test
    fun `보안 그룹은 허용한 CIDR 에서만 80 포트를 연다`() {
        val result = runPulumiProgram(mocks) { VpcEc2Stack.define(it, NetworkSettings(allowedHttpCidr = "203.0.113.7/32")) }

        val ingress = result.resourcesOf<SecurityGroup>().single().ingress().await().single()
        ingress.fromPort() shouldBe 80
        ingress.cidrBlocks() shouldContainExactly listOf("203.0.113.7/32")
    }
}
```

공용 헬퍼 — [`PulumiTestKit.kt`](../test-support/src/main/kotlin/io/hellopulumi/testsupport/PulumiTestKit.kt):

| 헬퍼 | 역할 |
|---|---|
| `runPulumiProgram(mocks, projectName, stackName) { ctx -> }` | Mock 환경에서 프로그램 실행, 오류 시 예외 |
| `result.resourcesOf<T>()` | 특정 타입의 리소스만 필터링 |
| `output.await()` | `Output<T>` 의 값을 동기적으로 추출 |
| `result.exported("name")` | 스택 export 값 조회 |

### 무엇을 테스트할까?

인프라 코드 테스트는 "AWS 가 동작하는가"가 아니라 **"내 코드가 의도한 설정을 선언하는가"** 를 검증합니다.

- 보안 규칙: 퍼블릭 접근 차단, IMDSv2, 허용 CIDR, 시크릿 여부
- 연결 관계: 서브넷이 올바른 VPC 에 속하는지, 정책이 올바른 버킷 ARN 을 가리키는지
- 설정 분기: 환경별 보존 기간, 기본값, 잘못된 입력 거부
- 계약: 다른 스택이 의존하는 export 값

### 시크릿 여부 검증

```kotlin
Internal.of(result.output("dbPassword")).isSecret().join() shouldBe true
```

## 실행과 커버리지

```bash
./gradlew test                       # 전체 단위 테스트
./gradlew :labs:04-vpc-ec2:test      # 특정 lab
./gradlew check                      # 테스트 + 커버리지 검증 (lab 별 라인 커버리지 80% 이상)
./gradlew koverHtmlReport            # labs/<lab>/build/reports/kover/html/index.html
```

`Main.kt` 는 실제 Pulumi 엔진이 있어야 실행되므로 커버리지 측정에서 제외합니다 (`*.MainKt`).
그래서 진입점은 얇게 유지하고 로직은 `XxxStack` / `XxxSettings` 로 옮기는 구조가 중요합니다.

## 더 나아가기

### 통합 테스트 — Automation API

Automation API 를 사용하면 JUnit 테스트 안에서 임시 스택을 만들어 실제로 배포하고, HTTP 요청으로 검증한 뒤 삭제할 수 있습니다.
비용과 시간이 들기 때문에 보통 야간 CI 나 릴리스 전 단계에서만 실행합니다.

### Policy as Code — CrossGuard

"모든 S3 버킷은 퍼블릭 접근 차단 필수", "t3.micro 보다 큰 인스턴스 금지" 같은 조직 규칙을 정책 팩으로 작성해
`pulumi preview --policy-pack <path>` 로 모든 프로젝트에 강제할 수 있습니다.

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [10. Lab 06 - StackReference](10-lab06-stack-reference.md) | [목차](../README.md#문서-학습-순서) | [12. 운영과 다음 단계](12-operations-and-next-steps.md) |
