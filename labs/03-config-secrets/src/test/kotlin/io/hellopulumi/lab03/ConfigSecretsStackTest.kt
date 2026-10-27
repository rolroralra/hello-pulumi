package io.hellopulumi.lab03

import com.pulumi.aws.cloudwatch.LogGroup
import com.pulumi.aws.ssm.Parameter
import com.pulumi.core.Output
import com.pulumi.core.internal.Internal
import com.pulumi.test.PulumiTest
import io.hellopulumi.testsupport.await
import io.hellopulumi.testsupport.resourcesOf
import io.hellopulumi.testsupport.runPulumiProgram
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class ConfigSecretsStackTest {

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()

    private val prod = LabSettings("prod", 30, "sre-team")

    @Test
    fun `환경별 이름과 보존 기간으로 로그 그룹을 만든다`() {
        val result = runPulumiProgram { ConfigSecretsStack.define(it, prod, Output.ofSecret("pw")) }

        with(result.resourcesOf<LogGroup>().single()) {
            name().await() shouldBe "/hello-pulumi/prod/app"
            retentionInDays().await().orElseThrow() shouldBe 30
            tags().await().orElseThrow() shouldContain ("Owner" to "sre-team")
        }
    }

    @Test
    fun `DB 비밀번호는 SecureString 파라미터에 시크릿으로 저장된다`() {
        val result = runPulumiProgram { ConfigSecretsStack.define(it, prod, Output.ofSecret("s3cr3t")) }

        val parameter = result.resourcesOf<Parameter>().single()
        parameter.name().await() shouldBe "/hello-pulumi/prod/db-password"
        parameter.type().await() shouldBe "SecureString"
        isSecret(result.output("dbPassword")) shouldBe true
    }

    private fun isSecret(output: Output<*>): Boolean =
        Internal.of(output).isSecret().join()
}
