package io.hellopulumi.lab04

import com.pulumi.aws.ec2.Instance
import com.pulumi.aws.ec2.RouteTable
import com.pulumi.aws.ec2.SecurityGroup
import com.pulumi.aws.ec2.Subnet
import com.pulumi.aws.ec2.Vpc
import com.pulumi.test.PulumiTest
import io.hellopulumi.testsupport.FakeCloudMocks
import io.hellopulumi.testsupport.await
import io.hellopulumi.testsupport.exported
import io.hellopulumi.testsupport.resourcesOf
import io.hellopulumi.testsupport.runPulumiProgram
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class VpcEc2StackTest {

    private val mocks = FakeCloudMocks(
        computed = mapOf(
            "aws:ec2/instance:Instance" to { _ -> mapOf("publicIp" to "203.0.113.10") },
        ),
        calls = mapOf(
            "aws:ssm/getParameter:getParameter" to mapOf(
                "arn" to "arn:aws:ssm:ap-northeast-2::parameter${VpcEc2Stack.AL2023_AMI_PARAMETER}",
                "id" to VpcEc2Stack.AL2023_AMI_PARAMETER,
                "insecureValue" to "ami-0123456789abcdef0",
                "name" to VpcEc2Stack.AL2023_AMI_PARAMETER,
                "region" to "ap-northeast-2",
                "type" to "String",
                "value" to "ami-0123456789abcdef0",
                "version" to 1,
            )
        ),
    )

    @AfterEach
    fun tearDown() = PulumiTest.cleanup()

    private fun run(settings: NetworkSettings = NetworkSettings()) =
        runPulumiProgram(mocks, stackName = "dev") { VpcEc2Stack.define(it, settings) }

    @Test
    fun `설정한 CIDR 로 VPC 와 퍼블릭 서브넷을 만든다`() {
        val result = run()

        result.resourcesOf<Vpc>().single().cidrBlock().await() shouldBe "10.0.0.0/16"
        with(result.resourcesOf<Subnet>().single()) {
            cidrBlock().await() shouldBe "10.0.1.0/24"
            mapPublicIpOnLaunch().await().orElseThrow() shouldBe true
            vpcId().await() shouldBe "lab04-vpc-id"
        }
    }

    @Test
    fun `퍼블릭 라우트 테이블은 인터넷 게이트웨이로 향한다`() {
        val route = run().resourcesOf<RouteTable>().single().routes().await().single()

        route.cidrBlock().orElseThrow() shouldBe "0.0.0.0/0"
        route.gatewayId().orElseThrow() shouldBe "lab04-igw-id"
    }

    @Test
    fun `보안 그룹은 허용한 CIDR 에서만 80 포트를 연다`() {
        val result = run(NetworkSettings(allowedHttpCidr = "203.0.113.7/32"))

        val ingress = result.resourcesOf<SecurityGroup>().single().ingress().await().single()
        ingress.fromPort() shouldBe 80
        ingress.toPort() shouldBe 80
        ingress.cidrBlocks() shouldContainExactly listOf("203.0.113.7/32")
    }

    @Test
    fun `EC2 는 조회한 AMI 와 IMDSv2 로 생성된다`() {
        val instance = run().resourcesOf<Instance>().single()

        instance.ami().await() shouldBe "ami-0123456789abcdef0"
        instance.instanceType().await() shouldBe "t3.micro"
        instance.metadataOptions().await().httpTokens().orElseThrow() shouldBe "required"
        instance.userData().await().orElseThrow() shouldContain "stack: dev"
    }

    @Test
    fun `웹 서버 URL 과 네트워크 정보를 export 한다`() {
        val result = run()

        result.exported("url") shouldBe "http://203.0.113.10"
        result.exported("vpcId") shouldBe "lab04-vpc-id"
        result.exported("vpcCidr") shouldBe "10.0.0.0/16"
        result.exported("publicSubnetId") shouldBe "lab04-public-subnet-id"
    }
}
