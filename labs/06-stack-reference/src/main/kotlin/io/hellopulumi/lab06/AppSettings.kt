package io.hellopulumi.lab06

/** Lab 06 스택 설정 */
data class AppSettings(
    val networkStack: String,
    val appPort: Int = DEFAULT_APP_PORT,
) {
    init {
        require(STACK_NAME_PATTERN.matches(networkStack)) {
            "networkStack 은 '<org>/<project>/<stack>' 형식이어야 합니다: '$networkStack'"
        }
        require(appPort in PORT_RANGE) { "appPort 는 $PORT_RANGE 범위여야 합니다: $appPort" }
    }

    companion object {
        const val DEFAULT_APP_PORT = 8080
        private val PORT_RANGE = 1..65535
        private val STACK_NAME_PATTERN = Regex("""^[\w.-]+/[\w.-]+/[\w.-]+$""")

        fun from(lookup: (String) -> String?): AppSettings {
            val networkStack = lookup("networkStack")
                ?: throw IllegalArgumentException(
                    "필수 설정 'networkStack' 이 없습니다. 예: pulumi config set networkStack organization/lab04-vpc-ec2/dev"
                )
            val port = lookup("appPort")?.let { raw ->
                raw.toIntOrNull() ?: throw IllegalArgumentException("appPort 는 정수여야 합니다: '$raw'")
            } ?: DEFAULT_APP_PORT
            return AppSettings(networkStack, port)
        }
    }
}
