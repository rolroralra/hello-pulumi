package io.hellopulumi.lab02

import com.pulumi.Context
import com.pulumi.asset.FileAsset
import com.pulumi.aws.s3.Bucket
import com.pulumi.aws.s3.BucketArgs
import com.pulumi.aws.s3.BucketObject
import com.pulumi.aws.s3.BucketObjectArgs
import com.pulumi.aws.s3.BucketOwnershipControls
import com.pulumi.aws.s3.BucketOwnershipControlsArgs
import com.pulumi.aws.s3.BucketPolicy
import com.pulumi.aws.s3.BucketPolicyArgs
import com.pulumi.aws.s3.BucketPublicAccessBlock
import com.pulumi.aws.s3.BucketPublicAccessBlockArgs
import com.pulumi.aws.s3.BucketWebsiteConfiguration
import com.pulumi.aws.s3.BucketWebsiteConfigurationArgs
import com.pulumi.aws.s3.inputs.BucketOwnershipControlsRuleArgs
import com.pulumi.aws.s3.inputs.BucketWebsiteConfigurationErrorDocumentArgs
import com.pulumi.aws.s3.inputs.BucketWebsiteConfigurationIndexDocumentArgs
import com.pulumi.aws.s3.inputs.PolicyDocumentArgs
import com.pulumi.core.Either
import com.pulumi.core.Output
import com.pulumi.resources.CustomResourceOptions
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile

/**
 * Lab 02: S3 정적 웹사이트.
 *
 * 핵심 학습 포인트
 * - Output.applyValue 로 "아직 모르는 값"(버킷 ARN)을 가공해 정책 JSON 만들기
 * - dependsOn 으로 명시적 의존성 표현 (Public Access Block 해제 → Bucket Policy 적용 순서)
 * - 로컬 파일을 FileAsset 으로 업로드
 */
object StaticWebsiteStack {

    const val DEFAULT_SITE_DIR = "www"
    const val INDEX_DOCUMENT = "index.html"
    const val ERROR_DOCUMENT = "error.html"

    fun define(ctx: Context, siteDir: Path) {
        val bucket = Bucket(
            "lab02-site",
            BucketArgs.builder()
                .forceDestroy(true)
                .tags(mapOf("Project" to ctx.projectName(), "Stack" to ctx.stackName()))
                .build()
        )

        val website = BucketWebsiteConfiguration(
            "lab02-site-website",
            BucketWebsiteConfigurationArgs.builder()
                .bucket(bucket.id())
                .indexDocument(BucketWebsiteConfigurationIndexDocumentArgs.builder().suffix(INDEX_DOCUMENT).build())
                .errorDocument(BucketWebsiteConfigurationErrorDocumentArgs.builder().key(ERROR_DOCUMENT).build())
                .build()
        )

        val ownership = BucketOwnershipControls(
            "lab02-site-ownership",
            BucketOwnershipControlsArgs.builder()
                .bucket(bucket.id())
                .rule(BucketOwnershipControlsRuleArgs.builder().objectOwnership("BucketOwnerEnforced").build())
                .build()
        )

        // 퍼블릭 "정책"은 허용해야 웹사이트로 공개할 수 있습니다. (ACL 은 계속 차단)
        val publicAccess = BucketPublicAccessBlock(
            "lab02-site-public-access",
            BucketPublicAccessBlockArgs.builder()
                .bucket(bucket.id())
                .blockPublicAcls(true)
                .ignorePublicAcls(true)
                .blockPublicPolicy(false)
                .restrictPublicBuckets(false)
                .build()
        )

        // bucket.arn() 은 Output<String>. applyValue 안에서만 실제 문자열을 다룰 수 있습니다.
        // AWS provider v7 의 policy 입력은 Either<String, PolicyDocumentArgs> 이므로 ofLeft 로 감쌉니다.
        BucketPolicy(
            "lab02-site-policy",
            BucketPolicyArgs.builder()
                .bucket(bucket.id())
                .policy(bucket.arn().applyValue { arn -> Either.ofLeft<String, PolicyDocumentArgs>(publicReadPolicy(arn)) })
                .build(),
            CustomResourceOptions.builder()
                .dependsOn(publicAccess, ownership) // 차단 해제가 먼저 끝나야 정책 적용이 성공합니다.
                .build()
        )

        uploadSite(bucket, siteDir)

        ctx.export("bucketName", bucket.bucket())
        // Output.format 은 여러 Output 을 문자열 템플릿으로 합칠 때 편리합니다.
        ctx.export("websiteUrl", Output.format("http://%s", website.websiteEndpoint()))
    }

    /** 사이트 디렉터리의 모든 파일을 S3 객체로 업로드합니다. */
    private fun uploadSite(bucket: Bucket, siteDir: Path): List<BucketObject> {
        require(Files.isDirectory(siteDir)) { "사이트 디렉터리가 없습니다: ${siteDir.toAbsolutePath()}" }

        return Files.walk(siteDir).use { paths ->
            paths.filter { it.isRegularFile() }
                .sorted()
                .toList()
        }.map { file ->
            val key = siteDir.relativize(file).toString().replace('\\', '/')
            BucketObject(
                "lab02-object-$key",
                BucketObjectArgs.builder()
                    .bucket(bucket.id())
                    .key(key)
                    .source(FileAsset(file.toString()))
                    .contentType(ContentTypes.of(file))
                    .build()
            )
        }
    }

    fun publicReadPolicy(bucketArn: String): String = """
        {
          "Version": "2012-10-17",
          "Statement": [
            {
              "Sid": "PublicReadGetObject",
              "Effect": "Allow",
              "Principal": "*",
              "Action": "s3:GetObject",
              "Resource": "$bucketArn/*"
            }
          ]
        }
    """.trimIndent()
}
