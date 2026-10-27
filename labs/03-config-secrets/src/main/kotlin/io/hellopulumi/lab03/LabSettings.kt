package io.hellopulumi.lab03

/**
 * 스택 설정(Pulumi Config)을 검증된 불변 객체로 변환합니다.
 *
 * Pulumi Config 는 단순 문자열 저장소이므로, 시스템 경계에서 한 번에 검증해 두면
 * 나머지 코드는 "항상 올바른 값"만 다룰 수 있습니다.
 */
data class LabSettings(
    val environment: String,
    val logRetentionDays: Int,
    val owner: String,
) {
    init {
        require(environment in ALLOWED_ENVIRONMENTS) {
            "environment 는 $ALLOWED_ENVIRONMENTS 중 하나여야 합니다: '$environment'"
        }
        require(logRetentionDays in ALLOWED_RETENTION_DAYS) {
            "logRetentionDays 는 CloudWatch 가 허용하는 값($ALLOWED_RETENTION_DAYS)이어야 합니다: $logRetentionDays"
        }
        require(owner.isNotBlank()) { "owner 는 비어 있을 수 없습니다." }
    }

    val isProduction: Boolean get() = environment == "prod"

    companion object {
        val ALLOWED_ENVIRONMENTS = setOf("dev", "stg", "prod")
        val ALLOWED_RETENTION_DAYS = listOf(1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365)
        const val DEFAULT_RETENTION_DAYS = 7
        const val DEFAULT_OWNER = "platform-team"

        /** @param lookup 설정 키로 값을 찾는 함수 (Pulumi Config 또는 테스트용 Map) */
        fun from(lookup: (String) -> String?): LabSettings {
            val environment = lookup("environment")
                ?: throw IllegalArgumentException(
                    "필수 설정 'environment' 가 없습니다. `pulumi config set environment dev` 로 설정하세요."
                )
            val retention = lookup("logRetentionDays")?.let { raw ->
                raw.toIntOrNull() ?: throw IllegalArgumentException("logRetentionDays 는 정수여야 합니다: '$raw'")
            } ?: DEFAULT_RETENTION_DAYS

            return LabSettings(
                environment = environment,
                logRetentionDays = retention,
                owner = lookup("owner") ?: DEFAULT_OWNER,
            )
        }
    }
}
