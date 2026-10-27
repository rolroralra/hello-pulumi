package io.hellopulumi.lab04

import com.pulumi.Context
import com.pulumi.aws.ec2.Instance
import com.pulumi.aws.ec2.InstanceArgs
import com.pulumi.aws.ec2.InternetGateway
import com.pulumi.aws.ec2.InternetGatewayArgs
import com.pulumi.aws.ec2.RouteTable
import com.pulumi.aws.ec2.RouteTableArgs
import com.pulumi.aws.ec2.RouteTableAssociation
import com.pulumi.aws.ec2.RouteTableAssociationArgs
import com.pulumi.aws.ec2.SecurityGroup
import com.pulumi.aws.ec2.SecurityGroupArgs
import com.pulumi.aws.ec2.Subnet
import com.pulumi.aws.ec2.SubnetArgs
import com.pulumi.aws.ec2.Vpc
import com.pulumi.aws.ec2.VpcArgs
import com.pulumi.aws.ec2.inputs.InstanceMetadataOptionsArgs
import com.pulumi.aws.ec2.inputs.RouteTableRouteArgs
import com.pulumi.aws.ec2.inputs.SecurityGroupEgressArgs
import com.pulumi.aws.ec2.inputs.SecurityGroupIngressArgs
import com.pulumi.aws.ssm.SsmFunctions
import com.pulumi.aws.ssm.inputs.GetParameterArgs
import com.pulumi.core.Output

/**
 * Lab 04: VPC + 퍼블릭 서브넷 + EC2(nginx).
 *
 * 핵심 학습 포인트
 * - 리소스 간 Output 연결만으로 생성 순서(의존성 그래프)가 자동 결정됨
 * - 데이터 소스(invoke)로 최신 AMI ID 조회
 */
object VpcEc2Stack {

    /** AWS 가 관리하는 "최신 Amazon Linux 2023 AMI" 퍼블릭 SSM 파라미터 */
    const val AL2023_AMI_PARAMETER = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
    private const val ANYWHERE = "0.0.0.0/0"
    private const val HTTP_PORT = 80

    fun define(ctx: Context, settings: NetworkSettings) {
        val stack = ctx.stackName()
        fun nameTag(name: String) = mapOf("Name" to "lab04-$stack-$name", "ManagedBy" to "pulumi")

        val vpc = Vpc(
            "lab04-vpc",
            VpcArgs.builder()
                .cidrBlock(settings.vpcCidr)
                .enableDnsHostnames(true)
                .enableDnsSupport(true)
                .tags(nameTag("vpc"))
                .build()
        )

        val igw = InternetGateway(
            "lab04-igw",
            InternetGatewayArgs.builder().vpcId(vpc.id()).tags(nameTag("igw")).build()
        )

        val publicSubnet = Subnet(
            "lab04-public-subnet",
            SubnetArgs.builder()
                .vpcId(vpc.id())
                .cidrBlock(settings.publicSubnetCidr)
                .mapPublicIpOnLaunch(true)
                .tags(nameTag("public-subnet"))
                .build()
        )

        val routeTable = RouteTable(
            "lab04-public-rt",
            RouteTableArgs.builder()
                .vpcId(vpc.id())
                .routes(RouteTableRouteArgs.builder().cidrBlock(ANYWHERE).gatewayId(igw.id()).build())
                .tags(nameTag("public-rt"))
                .build()
        )

        RouteTableAssociation(
            "lab04-public-rt-assoc",
            RouteTableAssociationArgs.builder()
                .subnetId(publicSubnet.id())
                .routeTableId(routeTable.id())
                .build()
        )

        val webSg = SecurityGroup(
            "lab04-web-sg",
            SecurityGroupArgs.builder()
                .vpcId(vpc.id())
                .description("Allow HTTP inbound")
                .ingress(
                    SecurityGroupIngressArgs.builder()
                        .protocol("tcp").fromPort(HTTP_PORT).toPort(HTTP_PORT)
                        .cidrBlocks(settings.allowedHttpCidr)
                        .description("HTTP")
                        .build()
                )
                .egress(
                    SecurityGroupEgressArgs.builder()
                        .protocol("-1").fromPort(0).toPort(0)
                        .cidrBlocks(ANYWHERE)
                        .build()
                )
                .tags(nameTag("web-sg"))
                .build()
        )

        // 데이터 소스 조회(invoke): 결과도 Output 으로 돌아옵니다.
        val amiId: Output<String> = SsmFunctions
            .getParameter(GetParameterArgs.builder().name(AL2023_AMI_PARAMETER).build())
            .applyValue { it.value() }

        val server = Instance(
            "lab04-web-server",
            InstanceArgs.builder()
                .ami(amiId)
                .instanceType(settings.instanceType)
                .subnetId(publicSubnet.id())
                .vpcSecurityGroupIds(webSg.id().applyValue { listOf(it) })
                .userData(UserData.nginx(stack))
                .userDataReplaceOnChange(true)
                // IMDSv2 강제 (보안 모범 사례)
                .metadataOptions(InstanceMetadataOptionsArgs.builder().httpTokens("required").build())
                .tags(nameTag("web"))
                .build()
        )

        // Lab 06 에서 StackReference 로 읽어 갈 값들
        ctx.export("vpcId", vpc.id())
        ctx.export("vpcCidr", vpc.cidrBlock())
        ctx.export("publicSubnetId", publicSubnet.id())
        ctx.export("instanceId", server.id())
        ctx.export("publicIp", server.publicIp())
        ctx.export("url", Output.format("http://%s", server.publicIp()))
    }
}
