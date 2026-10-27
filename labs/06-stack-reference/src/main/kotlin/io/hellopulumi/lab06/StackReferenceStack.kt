package io.hellopulumi.lab06

import com.pulumi.Context
import com.pulumi.aws.ec2.SecurityGroup
import com.pulumi.aws.ec2.SecurityGroupArgs
import com.pulumi.aws.vpc.SecurityGroupEgressRule
import com.pulumi.aws.vpc.SecurityGroupEgressRuleArgs
import com.pulumi.aws.vpc.SecurityGroupIngressRule
import com.pulumi.aws.vpc.SecurityGroupIngressRuleArgs
import com.pulumi.resources.StackReference

/**
 * Lab 06: "네트워크 스택"과 "애플리케이션 스택"의 분리.
 *
 * Lab 04 가 export 한 vpcId / vpcCidr 를 StackReference 로 읽어,
 * 그 VPC 안에 애플리케이션 전용 보안 그룹을 만듭니다.
 */
object StackReferenceStack {

    fun define(ctx: Context, settings: AppSettings) {
        val network = StackReference(settings.networkStack)
        val vpcId = network.requireString("vpcId")
        val vpcCidr = network.requireString("vpcCidr")

        val appSg = SecurityGroup(
            "lab06-app-sg",
            SecurityGroupArgs.builder()
                .vpcId(vpcId)
                .description("App port from inside the VPC only")
                .tags(mapOf("Name" to "lab06-${ctx.stackName()}-app-sg", "ManagedBy" to "pulumi"))
                .build()
        )

        // 인라인 ingress 대신 독립 규칙 리소스를 쓰면 규칙 단위로 추가/삭제가 깔끔합니다.
        SecurityGroupIngressRule(
            "lab06-app-ingress",
            SecurityGroupIngressRuleArgs.builder()
                .securityGroupId(appSg.id())
                .ipProtocol("tcp")
                .fromPort(settings.appPort)
                .toPort(settings.appPort)
                .cidrIpv4(vpcCidr)
                .description("App traffic from VPC")
                .build()
        )

        SecurityGroupEgressRule(
            "lab06-app-egress",
            SecurityGroupEgressRuleArgs.builder()
                .securityGroupId(appSg.id())
                .ipProtocol("-1")
                .cidrIpv4("0.0.0.0/0")
                .build()
        )

        ctx.export("referencedVpcId", vpcId)
        ctx.export("appSecurityGroupId", appSg.id())
    }
}
