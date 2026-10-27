# 01. Pulumi 란 무엇인가

## Infrastructure as Code (IaC)

클라우드 콘솔에서 클릭으로 서버와 네트워크를 만들면 빠르지만 다음 문제가 생깁니다.

- **재현 불가**: 같은 환경을 다시 만들기 어렵다 (dev 와 prod 가 미묘하게 다르다).
- **변경 이력 없음**: 누가, 언제, 왜 바꿨는지 알 수 없다.
- **리뷰 불가**: 변경 전에 동료가 검토할 방법이 없다.

IaC 는 인프라를 **코드로 선언**하고, 도구가 코드와 실제 클라우드 상태의 차이를 계산해 적용하는 방식입니다.
코드이므로 Git 으로 버전 관리하고, PR 로 리뷰하고, CI 에서 자동 배포할 수 있습니다.

## Pulumi 한 줄 정의

> **Pulumi 는 범용 프로그래밍 언어(TypeScript, Python, Go, C#, Java/Kotlin, YAML)로 인프라를 선언하는 오픈소스 IaC 도구**입니다.

```kotlin
// "S3 버킷이 하나 있어야 한다"는 선언
val bucket = Bucket("my-bucket", BucketArgs.builder().forceDestroy(true).build())
ctx.export("bucketName", bucket.bucket())
```

위 코드는 버킷을 "지금 만드는" 명령이 아니라 **원하는 상태(desired state)를 선언**하는 것입니다.
`pulumi up` 을 실행하면 Pulumi 엔진이 현재 상태와 비교해 생성/수정/삭제를 결정합니다.

## 왜 범용 언어인가?

| 범용 언어의 장점 | 예시 |
|---|---|
| 반복문, 조건문, 함수 | 환경별로 다른 개수의 리소스를 `for`/`if` 로 생성 |
| 타입 시스템과 IDE 지원 | 오타 난 속성은 컴파일 에러, 자동 완성으로 API 탐색 |
| 추상화와 재사용 | 클래스(ComponentResource)로 팀 표준 모듈 제공 |
| 기존 생태계 | Gradle, JUnit, 사내 라이브러리, 패키지 저장소를 그대로 사용 |
| 테스트 | 일반 단위 테스트 프레임워크로 인프라 코드 검증 |

## 다른 IaC 도구와 비교

| 항목 | Pulumi | Terraform / OpenTofu | AWS CDK | CloudFormation |
|---|---|---|---|---|
| 언어 | TS, Python, Go, C#, Java/Kotlin, YAML | HCL (DSL) | TS, Python, Java, C#, Go | YAML/JSON |
| 대상 | 멀티 클라우드 + SaaS | 멀티 클라우드 + SaaS | AWS 중심 | AWS 전용 |
| 실행 방식 | 엔진이 프로바이더 플러그인을 통해 API 직접 호출 | 프로바이더 플러그인을 통해 API 직접 호출 | CloudFormation 템플릿으로 합성(synth) 후 배포 | AWS 가 직접 실행 |
| 상태(State) | Pulumi Cloud / S3 / 로컬 파일 등 | HCP Terraform / S3 등 | CloudFormation 이 관리 | AWS 가 관리 |
| 시크릿 | 상태 파일 안에서 값 단위 암호화 기본 제공 | 상태에 평문 저장 (별도 대책 필요) | Secrets Manager 등 연동 | 동적 참조 |

> Pulumi 의 많은 프로바이더(AWS 포함)는 Terraform 프로바이더를 기반으로 생성(bridged)됩니다.
> 그래서 Terraform 에 익숙하다면 리소스 이름과 속성이 매우 친숙합니다.
> `pulumi convert --from terraform --language java` 로 HCL 을 Pulumi 코드로 변환할 수도 있습니다.

## Pulumi 생태계 한눈에 보기

| 구성 요소 | 역할 |
|---|---|
| **Pulumi CLI / 엔진** | `pulumi up/preview/destroy` 실행, 변경 계획 수립 (오픈소스) |
| **Language SDK** | 언어별 라이브러리. 이 프로젝트는 Java SDK 를 Kotlin 에서 사용 |
| **Provider** | AWS, Azure, GCP, Kubernetes 등 실제 API 를 호출하는 플러그인 |
| **Pulumi Cloud** | 상태 저장, 배포 히스토리, 팀 권한, 웹 콘솔 (개인 무료 티어 있음, 사용은 선택) |
| **Pulumi ESC** | 환경/시크릿/설정 중앙 관리 |
| **CrossGuard** | Policy as Code (예: "퍼블릭 S3 버킷 금지") |
| **Automation API** | CLI 없이 프로그램 안에서 Pulumi 실행 (사내 플랫폼 구축용) |

## 이 프로젝트에서 배울 것

1. Pulumi 의 동작 원리와 핵심 개념 (다음 문서)
2. Kotlin 으로 Pulumi 프로그램을 작성하는 방법
3. AWS 위에서 S3 → 정적 웹사이트 → 설정/시크릿 → VPC/EC2 → 컴포넌트 → 스택 참조 순서의 실습
4. 인프라 코드를 Mock 으로 단위 테스트하는 방법

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [README](../README.md) | [목차](../README.md#문서-학습-순서) | [02. 핵심 개념](02-core-concepts.md) |
