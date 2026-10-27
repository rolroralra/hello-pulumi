# 09. Lab 05 - ComponentResource 로 재사용 모듈 만들기

> 코드: [`labs/05-component-resource`](../labs/05-component-resource)

## 목표

- Lab 02 의 정적 웹사이트 구성을 **하나의 클래스(`StaticWebsite`)** 로 캡슐화한다.
- 같은 컴포넌트로 사이트 2개를 만들어 재사용성을 확인한다.
- `parent` 옵션으로 리소스 트리를 구성한다.

## ComponentResource 란?

여러 CustomResource 를 묶은 **논리적 리소스**입니다. 그 자체로는 클라우드에 아무것도 만들지 않지만

- 자식 리소스가 컴포넌트 아래 **트리**로 표시되어 구조가 명확해지고,
- 팀 표준(보안 설정, 태그 규칙 등)을 **라이브러리처럼 배포**할 수 있으며,
- 컴포넌트에 `protect`, `provider` 같은 옵션을 주면 자식에게 **상속**됩니다.

Terraform 의 module, AWS CDK 의 Construct 와 비슷한 개념입니다.

## 구조

```
pulumi:pulumi:Stack            lab05-component-resource-dev
├─ hellopulumi:web:StaticWebsite   docs-site
│  ├─ aws:s3:Bucket                    docs-site-bucket
│  ├─ aws:s3:BucketWebsiteConfiguration docs-site-website
│  ├─ aws:s3:BucketOwnershipControls   docs-site-ownership
│  ├─ aws:s3:BucketPublicAccessBlock   docs-site-public-access
│  ├─ aws:s3:BucketPolicy              docs-site-policy
│  ├─ aws:s3:BucketObject              docs-site-index.html
│  └─ aws:s3:BucketObject              docs-site-error.html
└─ hellopulumi:web:StaticWebsite   blog-site
   └─ ... (동일)
```

## 코드 살펴보기

```kotlin
class StaticWebsite(
    name: String,
    args: StaticWebsiteArgs,
    options: ComponentResourceOptions = ComponentResourceOptions.builder().build(),
) : ComponentResource("hellopulumi:web:StaticWebsite", name, options) {    // ① 타입 토큰

    val bucketName: Output<String>                                          // ② 공개 출력
    val websiteUrl: Output<String>

    init {
        val child = CustomResourceOptions.builder().parent(this).build()    // ③ 부모 지정

        val bucket = Bucket("$name-bucket", BucketArgs.builder()..., child) // ④ 이름 접두사
        // ... website, ownership, public access, policy, objects

        bucketName = bucket.bucket()
        websiteUrl = Output.format("http://%s", website.websiteEndpoint())

        registerOutputs(mapOf("bucketName" to bucketName, "websiteUrl" to websiteUrl))  // ⑤ 완료 신호
    }
}
```

| 번호 | 규칙 |
|---|---|
| ① | 타입 토큰은 `<package>:<module>:<type>` 형식. 조직 고유의 접두사를 사용 |
| ② | 사용하는 쪽에 필요한 값만 프로퍼티로 노출 (내부 리소스는 숨김) |
| ③ | 모든 자식에 `parent(this)` 지정 — 빠뜨리면 스택 루트에 붙어 트리가 깨짐 |
| ④ | 자식 이름에 컴포넌트 이름을 접두사로 붙여 **여러 인스턴스 간 충돌 방지** |
| ⑤ | `registerOutputs` 를 반드시 호출해 컴포넌트 등록을 마무리 |

사용하는 쪽은 매우 단순해집니다.

```kotlin
val docs = StaticWebsite("docs-site", StaticWebsiteArgs(indexHtml = "<h1>Docs</h1>", tags = tags))
val blog = StaticWebsite("blog-site", StaticWebsiteArgs(indexHtml = "<h1>Blog</h1>", tags = tags))
ctx.export("docsUrl", docs.websiteUrl)
```

## 실행

```bash
cd labs/05-component-resource
pulumi stack init dev
pulumi up
curl "$(pulumi stack output docsUrl)"
curl "$(pulumi stack output blogUrl)"
```

## 실험해 보기

1. 세 번째 사이트를 추가해 보기 — 코드 2줄
2. 컴포넌트에 `ComponentResourceOptions.builder().protect(true).build()` 를 주고 `pulumi destroy` → 자식까지 보호되는지 확인
3. `StaticWebsiteArgs` 에 `enableVersioning: Boolean` 옵션을 추가하고 조건부로 `BucketVersioning` 생성
4. 더 나아가: 컴포넌트를 별도 Gradle 모듈/Maven 패키지로 분리해 여러 프로젝트에서 공유

> Pulumi 는 컴포넌트를 다른 언어에서도 쓸 수 있게 패키징하는 **Multi-language Component** 도 지원합니다.

## 정리

```bash
pulumi destroy
pulumi stack rm dev
```

---

| ← 이전글 | 목차 | 다음글 → |
|:---|:---:|---:|
| [08. Lab 04 - VPC 와 EC2](08-lab04-vpc-ec2.md) | [목차](../README.md#문서-학습-순서) | [10. Lab 06 - StackReference](10-lab06-stack-reference.md) |
