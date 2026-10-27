package io.hellopulumi.lab02

import com.pulumi.aws.s3.BucketObject
import com.pulumi.aws.s3.BucketPolicy
import com.pulumi.aws.s3.BucketPublicAccessBlock
import com.pulumi.aws.s3.BucketWebsiteConfiguration
import com.pulumi.test.PulumiTest
import io.hellopulumi.testsupport.FakeCloudMocks
import io.hellopulumi.testsupport.await
import io.hellopulumi.testsupport.exported
import io.hellopulumi.testsupport.resourcesOf
import io.hellopulumi.testsupport.runPulumiProgram
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class StaticWebsiteStackTest {

    private val mocks = FakeCloudMocks(
        computed = mapOf(
            "aws:s3/bucket:Bucket" to { args ->
                mapOf("bucket" to "${args.name}-abc", "arn" to "arn:aws:s3:::${args.name}-abc")
            },
            "aws:s3/bucketWebsiteConfiguration:BucketWebsiteConfiguration" to { _ ->
                mapOf("websiteEndpoint" to "site.s3-website.ap-northeast-2.amazonaws.com")
            },
        )
    )

    @TempDir
    lateinit var siteDir: Path

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()

    private fun givenSite() {
        Files.writeString(siteDir.resolve("index.html"), "<h1>hi</h1>")
        Files.writeString(siteDir.resolve("error.html"), "<h1>404</h1>")
        Files.createDirectories(siteDir.resolve("css"))
        Files.writeString(siteDir.resolve("css/app.css"), "body{}")
    }

    @Test
    fun `사이트 디렉터리의 모든 파일을 올바른 Content-Type 으로 업로드한다`() {
        givenSite()

        val result = runPulumiProgram(mocks) { StaticWebsiteStack.define(it, siteDir) }

        val objects = result.resourcesOf<BucketObject>().map { it.key().await() to it.contentType().await() }
        objects shouldContainExactlyInAnyOrder listOf(
            "index.html" to "text/html; charset=utf-8",
            "error.html" to "text/html; charset=utf-8",
            "css/app.css" to "text/css; charset=utf-8",
        )
    }

    @Test
    fun `버킷 정책은 해당 버킷 객체에 대한 읽기만 허용한다`() {
        givenSite()

        val result = runPulumiProgram(mocks) { StaticWebsiteStack.define(it, siteDir) }

        val policy = result.resourcesOf<BucketPolicy>().single().policy().await()
        policy shouldContain "\"Action\": \"s3:GetObject\""
        policy shouldContain "arn:aws:s3:::lab02-site-abc/*"
    }

    @Test
    fun `ACL 은 차단하고 퍼블릭 정책만 허용한다`() {
        givenSite()

        val result = runPulumiProgram(mocks) { StaticWebsiteStack.define(it, siteDir) }

        with(result.resourcesOf<BucketPublicAccessBlock>().single()) {
            blockPublicAcls().await().orElseThrow() shouldBe true
            blockPublicPolicy().await().orElseThrow() shouldBe false
        }
    }

    @Test
    fun `인덱스와 에러 문서를 설정하고 웹사이트 URL 을 export 한다`() {
        givenSite()

        val result = runPulumiProgram(mocks) { StaticWebsiteStack.define(it, siteDir) }

        val website = result.resourcesOf<BucketWebsiteConfiguration>().single()
        website.indexDocument().await().orElseThrow().suffix() shouldBe "index.html"
        result.exported("websiteUrl") shouldBe "http://site.s3-website.ap-northeast-2.amazonaws.com"
    }

    @Test
    fun `사이트 디렉터리가 없으면 명확한 오류로 실패한다`() {
        val missing = siteDir.resolve("not-exists")

        val error = shouldThrow<Exception> {
            runPulumiProgram(mocks) { StaticWebsiteStack.define(it, missing) }
        }
        generateSequence<Throwable>(error) { it.cause }.any { it.message.orEmpty().contains("사이트 디렉터리가 없습니다") } shouldBe true
    }
}
