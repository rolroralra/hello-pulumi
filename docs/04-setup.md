# 04. 실습 환경 준비

## 1. JDK 21

Gradle toolchain 이 JDK 21 을 사용합니다. 로컬에 없으면 Gradle 이 자동으로 찾거나 설치를 안내합니다.

```bash
java -version          # 21.x 권장
```

## 2. Pulumi CLI 설치

```bash
# macOS
brew install pulumi/tap/pulumi

# Linux / macOS (설치 스크립트)
curl -fsSL https://get.pulumi.com | sh

# Windows
winget install pulumi

pulumi version         # v3.x
```

> Gradle 은 저장소의 Wrapper(`./gradlew`)를 사용하므로 따로 설치할 필요가 없습니다.

## 3. AWS 자격 증명

Pulumi AWS 프로바이더는 **AWS CLI 와 같은 자격 증명 체인**을 사용합니다.

```bash
# 방법 A: 프로필 (권장)
aws configure --profile pulumi-lab        # 또는 aws configure sso
export AWS_PROFILE=pulumi-lab

# 방법 B: 환경 변수
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...

# 확인
aws sts get-caller-identity
```

스택 설정으로 프로필을 고정할 수도 있습니다.

```bash
pulumi config set aws:profile pulumi-lab
```

### 필요한 권한

실습용 IAM 사용자/역할에는 다음 서비스 권한이 필요합니다.

| Lab | 서비스 |
|---|---|
| 01, 02, 05 | S3 |
| 03 | CloudWatch Logs, SSM Parameter Store (+ KMS 기본 키) |
| 04 | EC2, VPC, SSM (공개 AMI 파라미터 조회) |
| 06 | EC2 (Security Group) |

> 개인 학습 계정이라면 `PowerUserAccess` 로 시작해도 되지만, 운영 계정에서는 **최소 권한**으로 제한하세요.

### 리전

모든 lab 의 기본 리전은 `ap-northeast-2`(서울)이며 `Pulumi.yaml` 에 정의되어 있습니다. 스택별로 바꾸려면:

```bash
pulumi config set aws:region us-east-1
```

## 4. State Backend 로그인

둘 중 하나를 선택합니다.

### A. Pulumi Cloud (기본, 추천)

```bash
pulumi login           # 브라우저로 로그인 / 토큰 입력
```

- 웹 콘솔에서 스택, 리소스 그래프, 배포 히스토리 확인 가능
- 스택 전체 이름: `<내 계정 이름>/<project>/<stack>`

### B. 로컬 파일 (계정 없이)

```bash
pulumi login --local   # ~/.pulumi 에 상태 저장
export PULUMI_CONFIG_PASSPHRASE='로컬-시크릿-암호화-암호'
```

- 시크릿 암호화에 패스프레이즈를 사용합니다. 매번 입력하지 않으려면 위처럼 환경 변수로 설정하세요.
- 스택 전체 이름: `organization/<project>/<stack>`

### C. S3 (팀 자체 관리)

```bash
pulumi login s3://<state-bucket-name>
```

## 5. 동작 확인

```bash
# AWS 없이 Mock 으로 모든 lab 을 빌드/테스트
./gradlew check

# Pulumi 가 필요한 플러그인을 인식하는지 확인 (Pulumi CLI 가 내부적으로 호출하는 것과 동일)
./gradlew -q :labs:01-s3-bucket:run -PmainClass=com.pulumi.bootstrap.internal.Main --args=packages
# [{"resource":true,"name":"aws","version":"7.52.0"}]
```

## 비용과 정리 습관

- 실습이 끝나면 **항상** `pulumi destroy` → `pulumi stack rm <stack>` 순서로 정리합니다.
- Lab 04 의 EC2 와 퍼블릭 IPv4 는 시간당 과금됩니다.
- AWS Budgets 로 소액 예산 알림을 설정해 두면 안전합니다.

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [03. Kotlin 으로 Pulumi 사용하기](03-pulumi-with-kotlin.md) | [목차](../README.md#문서-학습-순서) | [05. Lab 01 - S3 버킷](05-lab01-s3-bucket.md) |
