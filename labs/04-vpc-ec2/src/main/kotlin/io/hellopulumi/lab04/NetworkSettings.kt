package io.hellopulumi.lab04

/** Lab 04 스택 설정 (검증된 불변 객체) */
data class NetworkSettings(
    val instanceType: String = DEFAULT_INSTANCE_TYPE,
    val vpcCidr: String = DEFAULT_VPC_CIDR,
    val publicSubnetCidr: String = DEFAULT_PUBLIC_SUBNET_CIDR,
    val allowedHttpCidr: String = DEFAULT_ALLOWED_HTTP_CIDR,
) {
    init {
        listOf(
            "vpcCidr" to vpcCidr,
            "publicSubnetCidr" to publicSubnetCidr,
            "allowedHttpCidr" to allowedHttpCidr,
        ).forEach { (key, value) ->
            require(CIDR_PATTERN.matches(value)) { "$key 는 CIDR 형식(a.b.c.d/n)이어야 합니다: '$value'" }
        }
        require(instanceType.isNotBlank()) { "instanceType 은 비어 있을 수 없습니다." }
    }

    companion object {
        const val DEFAULT_INSTANCE_TYPE = "t3.micro"
        const val DEFAULT_VPC_CIDR = "10.0.0.0/16"
        const val DEFAULT_PUBLIC_SUBNET_CIDR = "10.0.1.0/24"
        const val DEFAULT_ALLOWED_HTTP_CIDR = "0.0.0.0/0"

        private val CIDR_PATTERN =
            Regex("""^((25[0-5]|2[0-4]\d|1?\d?\d)\.){3}(25[0-5]|2[0-4]\d|1?\d?\d)/(3[0-2]|[12]?\d)$""")

        fun from(lookup: (String) -> String?): NetworkSettings = NetworkSettings(
            instanceType = lookup("instanceType") ?: DEFAULT_INSTANCE_TYPE,
            vpcCidr = lookup("vpcCidr") ?: DEFAULT_VPC_CIDR,
            publicSubnetCidr = lookup("publicSubnetCidr") ?: DEFAULT_PUBLIC_SUBNET_CIDR,
            allowedHttpCidr = lookup("allowedHttpCidr") ?: DEFAULT_ALLOWED_HTTP_CIDR,
        )
    }
}
