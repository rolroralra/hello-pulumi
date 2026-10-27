package io.hellopulumi.lab05

import com.pulumi.aws.s3.Bucket
import com.pulumi.aws.s3.BucketObject
import com.pulumi.aws.s3.BucketPolicy
import com.pulumi.test.PulumiTest
import io.hellopulumi.testsupport.FakeCloudMocks
import io.hellopulumi.testsupport.await
import io.hellopulumi.testsupport.exported
import io.hellopulumi.testsupport.resourcesOf
import io.hellopulumi.testsupport.runPulumiProgram
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class StaticWebsiteTest {

    private val mocks = FakeCloudMocks(
        computed = mapOf(
            "aws:s3/bucket:Bucket" to { args ->
                mapOf("bucket" to "${args.name}", "arn" to "arn:aws:s3:::${args.name}")
            },
            "aws:s3/bucketWebsiteConfiguration:BucketWebsiteConfiguration" to { args ->
                mapOf("websiteEndpoint" to "${args.name}.s3-website.ap-northeast-2.amazonaws.com")
            },
        )
    )

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()

    @Test
    fun `컴포넌트는 자식 리소스를 묶어 생성하고 출력값을 노출한다`() {
        lateinit var site: StaticWebsite
        val result = runPulumiProgram(mocks) {
            site = StaticWebsite("my-site", StaticWebsiteArgs(indexHtml = "<h1>hi</h1>"))
        }

        result.resourcesOf<StaticWebsite>() shouldHaveSize 1
        result.resourcesOf<Bucket>() shouldHaveSize 1
        site.bucketName.await() shouldBe "my-site-bucket"
        site.websiteUrl.await() shouldBe "http://my-site-website.s3-website.ap-northeast-2.amazonaws.com"
    }

    @Test
    fun `index 와 error 문서를 업로드한다`() {
        val result = runPulumiProgram(mocks) {
            StaticWebsite("my-site", StaticWebsiteArgs(indexHtml = "<h1>hi</h1>", errorHtml = "<h1>oops</h1>"))
        }

        result.resourcesOf<BucketObject>().map { it.key().await() to it.content().await().orElseThrow() } shouldContainExactlyInAnyOrder
            listOf("index.html" to "<h1>hi</h1>", "error.html" to "<h1>oops</h1>")
    }

    @Test
    fun `버킷 정책은 자기 버킷만 가리킨다`() {
        val result = runPulumiProgram(mocks) { StaticWebsite("a", StaticWebsiteArgs(indexHtml = "x")) }

        result.resourcesOf<BucketPolicy>().single().policy().await() shouldContain "arn:aws:s3:::a-bucket/*"
    }

    @Test
    fun `여러 인스턴스를 만들어도 리소스 이름이 충돌하지 않는다`() {
        val result = runPulumiProgram(mocks) { defineSites(it) }

        result.resourcesOf<StaticWebsite>() shouldHaveSize 2
        result.resourcesOf<Bucket>().map { it.bucket().await() } shouldContainExactlyInAnyOrder
            listOf("docs-site-bucket", "blog-site-bucket")
        result.exported("docsUrl") shouldBe "http://docs-site-website.s3-website.ap-northeast-2.amazonaws.com"
        result.exported("blogUrl") shouldBe "http://blog-site-website.s3-website.ap-northeast-2.amazonaws.com"
    }

    @Test
    fun `index 문서가 비어 있으면 거부한다`() {
        shouldThrow<IllegalArgumentException> { StaticWebsiteArgs(indexHtml = " ") }
    }
}
