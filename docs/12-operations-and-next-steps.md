# 12. 운영과 다음 단계

## 전체 정리 체크리스트

실습 후 남은 리소스가 없도록 확인합니다.

```bash
# 백엔드에 남아 있는 모든 스택 확인
pulumi stack ls --all

# 각 lab 디렉터리에서 (Lab 06 → Lab 04 순서 주의)
pulumi stack select <stack>
pulumi destroy --yes
pulumi stack rm <stack> --yes
```

AWS 쪽에서도 한 번 더 확인합니다.

```bash
aws resourcegroupstaggingapi get-resources --tag-filters Key=ManagedBy,Values=pulumi \
  --query 'ResourceTagMappingList[].ResourceARN'
```

## 자주 쓰는 운영 명령

| 상황 | 명령 |
|---|---|
| 변경 내용을 자세히 보기 | `pulumi preview --diff` |
| 콘솔에서 누가 바꿨는지 감지 (drift) | `pulumi refresh` / `pulumi preview --refresh` |
| 기존 리소스를 Pulumi 관리로 가져오기 | `pulumi import aws:s3/bucket:Bucket my-bucket <버킷이름>` |
| 특정 리소스만 대상으로 실행 | `pulumi up --target <URN>` |
| 리소스 강제 교체 | `pulumi up --replace <URN>` |
| 상태에서만 제거 (실제 리소스는 유지) | `pulumi state delete <URN>` |
| 중단된 배포로 인한 잠금/대기 작업 정리 | `pulumi cancel` / `pulumi refresh` |
| 배포 이력 | `pulumi stack history` |
| 상세 로그 | `pulumi up -v=9 --logtostderr` |

## 트러블슈팅

| 증상 | 원인 / 해결 |
|---|---|
| `no Pulumi.yaml project file found` | lab 디렉터리(`labs/0x-...`) 안에서 실행해야 합니다 |
| `Program run without the Pulumi engine available` | `./gradlew run` 으로 직접 실행한 경우. `pulumi up` 으로 실행하세요 |
| `passphrase must be set` | 로컬 백엔드 사용 시 `PULUMI_CONFIG_PASSPHRASE` 설정 |
| `NoCredentialProviders` / `ExpiredToken` | `aws sts get-caller-identity` 로 자격 증명 확인, SSO 재로그인 |
| 버킷 정책 적용 시 `AccessDenied` | Public Access Block 해제 전에 정책이 적용됨 → `dependsOn` 확인 (Lab 02) |
| 첫 실행이 느림 | Gradle 의존성과 AWS 프로바이더 플러그인 다운로드. 이후 캐시됨 |

## CI/CD 예시 — GitHub Actions

PR 에서는 `preview` 결과를 코멘트로 남기고, main 머지 시 `up` 을 실행하는 패턴이 일반적입니다.

```yaml
name: pulumi
on:
  pull_request:
  push:
    branches: [main]

permissions:
  id-token: write        # AWS OIDC
  contents: read
  pull-requests: write

jobs:
  lab01:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - run: ./gradlew check
      - uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
          aws-region: ap-northeast-2
      - uses: pulumi/actions@v6
        with:
          command: ${{ github.event_name == 'pull_request' && 'preview' || 'up' }}
          stack-name: <org>/lab01-s3-bucket/dev
          work-dir: labs/01-s3-bucket
          comment-on-pr: true
        env:
          PULUMI_ACCESS_TOKEN: ${{ secrets.PULUMI_ACCESS_TOKEN }}
```

> 장기 Access Key 대신 **OIDC 로 IAM Role 을 assume** 하는 방식을 권장합니다.
> 액션 버전은 사용 시점의 최신 메이저 버전을 확인하세요.

## 멀티 클라우드로 확장하기

이 프로젝트는 AWS 에 집중했지만, 프로그래밍 모델(Project / Stack / Resource / Output)은 모든 프로바이더에서 동일합니다.
의존성 하나를 추가하면 같은 방식으로 다른 클라우드를 다룰 수 있습니다.

```kotlin
// build.gradle.kts
implementation("com.pulumi:azure-native:<version>")
implementation("com.pulumi:gcp:<version>")
implementation("com.pulumi:kubernetes:<version>")
```

```kotlin
// GCP 버킷 — AWS 와 구조가 같다
val bucket = com.pulumi.gcp.storage.Bucket(
    "my-bucket",
    BucketArgs.builder().location("ASIA-NORTHEAST3").build()
)
ctx.export("url", bucket.url())
```

하나의 프로그램 안에서 여러 프로바이더를 동시에 사용할 수도 있습니다 (예: AWS 에 EKS 생성 → Kubernetes 프로바이더로 앱 배포).

## 다음 학습 주제

| 주제 | 내용 |
|---|---|
| Explicit Provider | `com.pulumi.aws.Provider` 로 멀티 리전/멀티 계정 배포 |
| Transformations | 모든 리소스에 공통 태그 자동 부여 (`StackOptions` 의 resource transformations) |
| Pulumi ESC | 환경별 설정과 시크릿을 중앙에서 관리하고 여러 스택에서 공유 |
| Automation API | 셀프서비스 인프라 포털, 통합 테스트 자동화 |
| CrossGuard | 조직 정책을 코드로 강제 |
| `pulumi convert` | 기존 Terraform/CloudFormation 코드를 Pulumi 로 이관 |

## 참고 자료

- Pulumi 공식 문서: https://www.pulumi.com/docs/
- Java SDK 가이드: https://www.pulumi.com/docs/iac/languages-sdks/java/
- AWS Provider API: https://www.pulumi.com/registry/packages/aws/api-docs/
- Pulumi Java SDK 소스: https://github.com/pulumi/pulumi-java
- 예제 모음: https://github.com/pulumi/examples

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [11. 인프라 코드 테스트](11-testing.md) | [목차](../README.md#문서-학습-순서) | [README (처음으로)](../README.md) |
