package io.hellopulumi.lab01

import com.pulumi.aws.s3.Bucket
import com.pulumi.aws.s3.BucketPublicAccessBlock
import com.pulumi.aws.s3.BucketVersioning
import com.pulumi.test.PulumiTest
import io.hellopulumi.testsupport.FakeCloudMocks
import io.hellopulumi.testsupport.await
import io.hellopulumi.testsupport.exported
import io.hellopulumi.testsupport.resourcesOf
import io.hellopulumi.testsupport.runPulumiProgram
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class S3BucketStackTest {

    private val mocks = FakeCloudMocks(
        computed = mapOf(
            "aws:s3/bucket:Bucket" to { args ->
                val name = "${args.name}-1234567"
                mapOf("bucket" to name, "arn" to "arn:aws:s3:::$name")
            }
        )
    )

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()

    @Test
    fun `버킷에는 프로젝트와 스택 태그가 붙는다`() {
        val result = runPulumiProgram(mocks, projectName = "lab01", stackName = "dev") { S3BucketStack.define(it) }

        val bucket = result.resourcesOf<Bucket>().single()
        val tags = bucket.tags().await().orElseThrow()

        tags shouldContain ("Project" to "lab01")
        tags shouldContain ("Stack" to "dev")
        tags shouldContain ("ManagedBy" to "pulumi")
    }

    @Test
    fun `버전 관리가 활성화된다`() {
        val result = runPulumiProgram(mocks) { S3BucketStack.define(it) }

        val versioning = result.resourcesOf<BucketVersioning>().single()

        versioning.versioningConfiguration().await().status() shouldBe S3BucketStack.VERSIONING_ENABLED
    }

    @Test
    fun `퍼블릭 접근은 모두 차단된다`() {
        val result = runPulumiProgram(mocks) { S3BucketStack.define(it) }

        val block = result.resourcesOf<BucketPublicAccessBlock>()
        block shouldHaveSize 1
        with(block.single()) {
            blockPublicAcls().await().orElseThrow() shouldBe true
            blockPublicPolicy().await().orElseThrow() shouldBe true
            ignorePublicAcls().await().orElseThrow() shouldBe true
            restrictPublicBuckets().await().orElseThrow() shouldBe true
        }
    }

    @Test
    fun `버킷 이름과 ARN 을 export 한다`() {
        val result = runPulumiProgram(mocks) { S3BucketStack.define(it) }

        result.exported("bucketName") shouldBe "lab01-bucket-1234567"
        result.exported("bucketArn") shouldBe "arn:aws:s3:::lab01-bucket-1234567"
    }
}
