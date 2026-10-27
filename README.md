# hello-pulumi

**Pulumi + Kotlin 으로 AWS 인프라를 코드로 관리하는 실습 프로젝트**입니다.
Pulumi 의 개념을 문서로 학습하고, 6개의 단계별 실습(Lab)으로 직접 AWS 리소스를 만들고 지워 봅니다.

> Pulumi 는 AWS 뿐 아니라 Azure, Google Cloud, Kubernetes 등 수백 개의 프로바이더를 같은 방식으로 다룹니다.
> 이 프로젝트는 학습 비용을 줄이기 위해 **AWS 하나에 집중**합니다.

## 기술 스택

| 구분 | 버전 | 비고 |
|---|---|---|
| Kotlin | 2.4.21 | 최신 안정 버전 |
| JDK | 21 (LTS) | Gradle toolchain 으로 지정 |
| Gradle | 9.8.2 | Wrapper 포함 (`./gradlew`) |
| Pulumi CLI | 3.x (작성 시점 3.272.0) | 별도 설치 필요 |
| Pulumi Java SDK | `com.pulumi:pulumi:1.37.8` | Kotlin 에서 그대로 사용 |
| Pulumi AWS Provider | `com.pulumi:aws:7.52.0` | AWS provider v7 |
| 테스트 | JUnit 6.1.3, Kotest assertions 6.2.6, Kover 0.9.11 | Pulumi Mock 기반 단위 테스트 |

버전은 모두 [`gradle/libs.versions.toml`](gradle/libs.versions.toml) 한 곳에서 관리합니다.

## 문서 (학습 순서)

| # | 문서 | 내용 |
|---|---|---|
| 01 | [Pulumi 란 무엇인가](docs/01-what-is-pulumi.md) | IaC, Pulumi 의 특징, Terraform/CDK 와 비교 |
| 02 | [핵심 개념](docs/02-core-concepts.md) | Project, Stack, Resource, Input/Output, State, 엔진 동작 원리 |
| 03 | [Kotlin 으로 Pulumi 사용하기](docs/03-pulumi-with-kotlin.md) | Java SDK + Kotlin, Gradle 구조, 상호운용 팁 |
| 04 | [실습 환경 준비](docs/04-setup.md) | Pulumi CLI, AWS 자격 증명, 백엔드 로그인 |
| 05 | [Lab 01 - S3 버킷](docs/05-lab01-s3-bucket.md) | 첫 번째 `pulumi up` |
| 06 | [Lab 02 - S3 정적 웹사이트](docs/06-lab02-static-website.md) | Output 가공, 명시적 의존성, 파일 업로드 |
| 07 | [Lab 03 - Config 와 Secret](docs/07-lab03-config-secrets.md) | 스택별 설정, 시크릿 암호화 |
| 08 | [Lab 04 - VPC 와 EC2](docs/08-lab04-vpc-ec2.md) | 네트워크 구성, 데이터 소스 조회 |
| 09 | [Lab 05 - ComponentResource](docs/09-lab05-component-resource.md) | 재사용 가능한 인프라 모듈 |
| 10 | [Lab 06 - StackReference](docs/10-lab06-stack-reference.md) | 스택 간 출력값 공유 |
| 11 | [인프라 코드 테스트](docs/11-testing.md) | Mock 기반 단위 테스트, 커버리지 |
| 12 | [운영과 다음 단계](docs/12-operations-and-next-steps.md) | 정리, drift, CI/CD, 멀티 클라우드 |

## 프로젝트 구조

```
hello-pulumi/
├── README.md
├── docs/                         # 학습 문서 (01 ~ 12)
├── settings.gradle.kts           # 멀티 프로젝트 설정 (lab 목록)
├── gradle/libs.versions.toml     # 버전 카탈로그
├── buildSrc/                     # 공통 빌드 규칙 (pulumi-kotlin-lab 컨벤션 플러그인)
├── test-support/                 # 테스트 공용 Pulumi Mock
└── labs/
    ├── 01-s3-bucket/             # 각 lab = Pulumi 프로젝트(Pulumi.yaml) + Gradle 서브프로젝트
    ├── 02-static-website/
    ├── 03-config-secrets/
    ├── 04-vpc-ec2/
    ├── 05-component-resource/
    └── 06-stack-reference/
```

## 빠른 시작

```bash
# 0. 사전 준비: JDK 21, Pulumi CLI, AWS 자격 증명 (docs/04-setup.md 참고)
brew install pulumi/tap/pulumi
aws sts get-caller-identity

# 1. 빌드 & 테스트 (AWS 없이 Mock 으로 실행)
./gradlew check

# 2. 첫 번째 실습 배포
cd labs/01-s3-bucket
pulumi login --local          # 또는 pulumi login (Pulumi Cloud)
pulumi stack init dev
pulumi up

# 3. 정리 (중요! 과금 방지)
pulumi destroy
pulumi stack rm dev
```

## 비용 주의

- Lab 01~03, 05, 06 은 비용이 거의 발생하지 않습니다 (S3, CloudWatch Logs, SSM, Security Group).
- **Lab 04 는 EC2 인스턴스와 퍼블릭 IPv4 를 사용하므로 시간당 과금**됩니다. 실습 후 반드시 `pulumi destroy` 하세요.

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| - | [목차](#문서-학습-순서) | [01. Pulumi 란 무엇인가](docs/01-what-is-pulumi.md) |
