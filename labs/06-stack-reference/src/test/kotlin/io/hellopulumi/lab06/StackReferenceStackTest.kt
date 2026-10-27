package io.hellopulumi.lab06

import com.pulumi.aws.ec2.SecurityGroup
import com.pulumi.aws.vpc.SecurityGroupIngressRule
import com.pulumi.test.PulumiTest
import io.hellopulumi.testsupport.FakeCloudMocks
import io.hellopulumi.testsupport.await
import io.hellopulumi.testsupport.exported
import io.hellopulumi.testsupport.resourcesOf
import io.hellopulumi.testsupport.runPulumiProgram
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class StackReferenceStackTest {

    private fun mocksWithNetworkOutputs(outputs: Map<String, Any>) = FakeCloudMocks(
        computed = mapOf("pulumi:pulumi:StackReference" to { _ -> mapOf("outputs" to outputs) })
    )

    private val networkOutputs = mapOf("vpcId" to "vpc-0abc", "vpcCidr" to "10.0.0.0/16")
    private val settings = AppSettings("organization/lab04-vpc-ec2/dev", 9090)

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()

    @Test
    fun `참조한 VPC 안에 보안 그룹을 만든다`() {
        val result = runPulumiProgram(mocksWithNetworkOutputs(networkOutputs)) {
            StackReferenceStack.define(it, settings)
        }

        result.resourcesOf<SecurityGroup>().single().vpcId().await() shouldBe "vpc-0abc"
        result.exported("referencedVpcId") shouldBe "vpc-0abc"
    }

    @Test
    fun `앱 포트는 VPC CIDR 에서만 허용한다`() {
        val result = runPulumiProgram(mocksWithNetworkOutputs(networkOutputs)) {
            StackReferenceStack.define(it, settings)
        }

        with(result.resourcesOf<SecurityGroupIngressRule>().single()) {
            fromPort().await().orElseThrow() shouldBe 9090
            toPort().await().orElseThrow() shouldBe 9090
            cidrIpv4().await().orElseThrow() shouldBe "10.0.0.0/16"
        }
    }

    @Test
    fun `참조 스택에 필요한 출력이 없으면 실패한다`() {
        shouldThrow<Exception> {
            runPulumiProgram(mocksWithNetworkOutputs(mapOf("vpcId" to "vpc-0abc"))) {
                StackReferenceStack.define(it, settings)
            }
        }
    }
}
