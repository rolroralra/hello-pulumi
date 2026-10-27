# 07. Lab 03 - Config 와 Secret

> 코드: [`labs/03-config-secrets`](../labs/03-config-secrets)

## 목표

- **같은 코드**를 `dev` / `prod` 두 스택에 **다른 설정**으로 배포한다.
- 시크릿을 암호화해 저장하고, AWS SSM SecureString 으로 안전하게 전달한다.
- 설정을 시스템 경계에서 검증한다.

## 만들 리소스

| 리소스 | dev | prod |
|---|---|---|
| CloudWatch LogGroup | `/hello-pulumi/dev/app`, 보존 3일 | `/hello-pulumi/prod/app`, 보존 30일 |
| SSM Parameter (SecureString) | `/hello-pulumi/dev/db-password` | `/hello-pulumi/prod/db-password` |
| Owner 태그 | `platform-team` (기본값) | `sre-team` |

## 설정 선언과 스택 파일

`Pulumi.yaml` 에서 프로젝트 설정의 **타입, 기본값, 설명**을 선언합니다.

```yaml
config:
  environment:
    type: string
    description: "배포 환경 이름 (dev | stg | prod)"
  logRetentionDays:
    type: integer
    default: 7
```

스택별 값은 `Pulumi.<stack>.yaml` 에 있습니다 (이 저장소에 포함).

```yaml
# Pulumi.prod.yaml
config:
  lab03-config-secrets:environment: prod
  lab03-config-secrets:logRetentionDays: "30"
  lab03-config-secrets:owner: sre-team
```

> 키 앞의 `lab03-config-secrets:` 는 프로젝트 이름 네임스페이스입니다. CLI 에서는 생략 가능합니다.

## 코드 살펴보기

### 1. 설정 → 검증된 불변 객체

```kotlin
// Main.kt
val config = ctx.config()
val settings = LabSettings.from { key -> config.get(key).orElse(null) }
val dbPassword: Output<String> = config.requireSecret("dbPassword")
```

```kotlin
// LabSettings.kt
data class LabSettings(val environment: String, val logRetentionDays: Int, val owner: String) {
    init {
        require(environment in ALLOWED_ENVIRONMENTS) { ... }
        require(logRetentionDays in ALLOWED_RETENTION_DAYS) { ... }   // CloudWatch 허용 값만
    }
}
```

잘못된 값은 AWS API 를 호출하기 전에 `pulumi preview` 단계에서 바로 실패합니다.

### 2. 시크릿 다루기

```kotlin
Parameter(
    "lab03-db-password",
    ParameterArgs.builder()
        .name("$prefix/db-password")
        .type(ParameterType.SecureString)
        .value(dbPassword)                 // 시크릿 Output 을 그대로 전달
        .build()
)
ctx.export("dbPassword", dbPassword)       // export 해도 [secret] 으로 표시
```

- `requireSecret` 으로 읽은 값은 **시크릿 Output** 입니다. 이 값을 입력으로 받은 속성은 상태 파일에서도 암호화됩니다.
- `applyValue` 로 가공해도 시크릿 표시가 전파됩니다.
- 일반 값을 시크릿으로 만들려면 `Output.ofSecret(value)` 를 사용합니다.

## 실행

```bash
cd labs/03-config-secrets

# dev 스택
pulumi stack init dev
pulumi config set --secret dbPassword 'dev-p@ssw0rd'
cat Pulumi.dev.yaml               # dbPassword 가 secure: v1:... 로 암호화되어 있음
pulumi up
pulumi stack output               # dbPassword  [secret]
pulumi stack output dbPassword --show-secrets

# prod 스택 — 같은 코드, 다른 설정
pulumi stack init prod
pulumi config set --secret dbPassword 'prod-very-strong-password'
pulumi up

# 확인
aws logs describe-log-groups --log-group-name-prefix /hello-pulumi \
  --query 'logGroups[].[logGroupName,retentionInDays]' --output table
aws ssm get-parameter --name /hello-pulumi/prod/db-password --with-decryption
```

## 실험해 보기

1. `pulumi config set logRetentionDays 10` → 검증 실패 메시지 확인
2. `pulumi config rm environment` → 설정 안내 메시지 확인
3. `pulumi stack select dev && pulumi config` → 스택별 설정 비교
4. 시크릿 공급자 변경: `pulumi stack change-secrets-provider awskms://alias/my-key` (KMS 키로 암호화)

## 정리

```bash
pulumi stack select dev  && pulumi destroy && pulumi stack rm dev
pulumi stack select prod && pulumi destroy && pulumi stack rm prod
```

> `pulumi stack rm` 은 해당 스택의 설정 파일(`Pulumi.<stack>.yaml`)도 삭제합니다. 저장소에 포함된 파일이므로
> 다시 실습하려면 `git checkout` 으로 복원하거나 `--preserve-config` 옵션을 사용하세요.

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [06. Lab 02 - S3 정적 웹사이트](06-lab02-static-website.md) | [목차](../README.md#문서-학습-순서) | [08. Lab 04 - VPC 와 EC2](08-lab04-vpc-ec2.md) |
