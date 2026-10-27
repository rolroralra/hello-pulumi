package io.hellopulumi.lab01

import com.pulumi.Context
import com.pulumi.aws.s3.Bucket
import com.pulumi.aws.s3.BucketArgs
import com.pulumi.aws.s3.BucketPublicAccessBlock
import com.pulumi.aws.s3.BucketPublicAccessBlockArgs
import com.pulumi.aws.s3.BucketVersioning
import com.pulumi.aws.s3.BucketVersioningArgs
import com.pulumi.aws.s3.inputs.BucketVersioningVersioningConfigurationArgs

/**
 * Lab 01: 가장 단순한 Pulumi 프로그램.
 *
 * - 비공개 S3 버킷 1개 (+ 버전 관리, 퍼블릭 접근 차단)
 * - 버킷 이름/ARN 을 스택 출력(export)으로 노출
 */
object S3BucketStack {

    const val VERSIONING_ENABLED = "Enabled"

    fun define(ctx: Context) {
        val tags = commonTags(ctx)

        // 1) 버킷: 이름을 지정하지 않으면 Pulumi 가 "lab01-bucket-xxxxxxx" 처럼 랜덤 접미사를 붙입니다 (auto-naming).
        val bucket = Bucket(
            "lab01-bucket",
            BucketArgs.builder()
                .forceDestroy(true) // 실습용: 객체가 남아 있어도 destroy 가능
                .tags(tags)
                .build()
        )

        // 2) 버전 관리: bucket.id() 는 Output<String> — 버킷이 생성된 "이후"에 확정되는 값입니다.
        BucketVersioning(
            "lab01-bucket-versioning",
            BucketVersioningArgs.builder()
                .bucket(bucket.id())
                .versioningConfiguration(
                    BucketVersioningVersioningConfigurationArgs.builder()
                        .status(VERSIONING_ENABLED)
                        .build()
                )
                .build()
        )

        // 3) 퍼블릭 접근 전면 차단
        BucketPublicAccessBlock(
            "lab01-bucket-public-access-block",
            BucketPublicAccessBlockArgs.builder()
                .bucket(bucket.id())
                .blockPublicAcls(true)
                .blockPublicPolicy(true)
                .ignorePublicAcls(true)
                .restrictPublicBuckets(true)
                .build()
        )

        // 4) 스택 출력: `pulumi stack output bucketName` 으로 조회
        ctx.export("bucketName", bucket.bucket())
        ctx.export("bucketArn", bucket.arn())
    }

    fun commonTags(ctx: Context): Map<String, String> = mapOf(
        "Project" to ctx.projectName(),
        "Stack" to ctx.stackName(),
        "ManagedBy" to "pulumi",
    )
}
