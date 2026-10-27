package io.hellopulumi.lab05

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
import com.pulumi.resources.ComponentResource
import com.pulumi.resources.ComponentResourceOptions
import com.pulumi.resources.CustomResourceOptions

/**
 * S3 정적 웹사이트를 하나의 논리 단위로 묶은 ComponentResource.
 *
 * - 자식 리소스는 모두 parent(this) 로 등록되어 `pulumi stack --show-urns` / 콘솔에서 트리로 보입니다.
 * - 자식 리소스 이름에 컴포넌트 이름을 접두사로 붙여 여러 인스턴스를 만들어도 충돌하지 않습니다.
 */
class StaticWebsite(
    name: String,
    args: StaticWebsiteArgs,
    options: ComponentResourceOptions = ComponentResourceOptions.builder().build(),
) : ComponentResource(TYPE, name, options) {

    val bucketName: Output<String>
    val websiteUrl: Output<String>

    init {
        val child = CustomResourceOptions.builder().parent(this).build()

        val bucket = Bucket(
            "$name-bucket",
            BucketArgs.builder().forceDestroy(true).tags(args.tags).build(),
            child,
        )

        val website = BucketWebsiteConfiguration(
            "$name-website",
            BucketWebsiteConfigurationArgs.builder()
                .bucket(bucket.id())
                .indexDocument(BucketWebsiteConfigurationIndexDocumentArgs.builder().suffix(INDEX_KEY).build())
                .errorDocument(BucketWebsiteConfigurationErrorDocumentArgs.builder().key(ERROR_KEY).build())
                .build(),
            child,
        )

        val ownership = BucketOwnershipControls(
            "$name-ownership",
            BucketOwnershipControlsArgs.builder()
                .bucket(bucket.id())
                .rule(BucketOwnershipControlsRuleArgs.builder().objectOwnership("BucketOwnerEnforced").build())
                .build(),
            child,
        )

        val publicAccess = BucketPublicAccessBlock(
            "$name-public-access",
            BucketPublicAccessBlockArgs.builder()
                .bucket(bucket.id())
                .blockPublicAcls(true)
                .ignorePublicAcls(true)
                .blockPublicPolicy(false)
                .restrictPublicBuckets(false)
                .build(),
            child,
        )

        BucketPolicy(
            "$name-policy",
            BucketPolicyArgs.builder()
                .bucket(bucket.id())
                .policy(bucket.arn().applyValue { arn -> Either.ofLeft<String, PolicyDocumentArgs>(publicReadPolicy(arn)) })
                .build(),
            CustomResourceOptions.builder().parent(this).dependsOn(publicAccess, ownership).build(),
        )

        listOf(INDEX_KEY to args.indexHtml, ERROR_KEY to args.errorHtml).forEach { (key, html) ->
            BucketObject(
                "$name-$key",
                BucketObjectArgs.builder()
                    .bucket(bucket.id())
                    .key(key)
                    .content(html)
                    .contentType(HTML_CONTENT_TYPE)
                    .build(),
                child,
            )
        }

        bucketName = bucket.bucket()
        websiteUrl = Output.format("http://%s", website.websiteEndpoint())

        // 컴포넌트의 출력 등록: 이 호출로 컴포넌트 생성이 "완료"되었음을 엔진에 알립니다.
        registerOutputs(mapOf("bucketName" to bucketName, "websiteUrl" to websiteUrl))
    }

    companion object {
        /** <package>:<module>:<type> 형식의 컴포넌트 타입 토큰 */
        const val TYPE = "hellopulumi:web:StaticWebsite"
        const val INDEX_KEY = "index.html"
        const val ERROR_KEY = "error.html"
        private const val HTML_CONTENT_TYPE = "text/html; charset=utf-8"

        fun publicReadPolicy(bucketArn: String): String = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": "s3:GetObject",
                  "Resource": "$bucketArn/*"
                }
              ]
            }
        """.trimIndent()
    }
}
