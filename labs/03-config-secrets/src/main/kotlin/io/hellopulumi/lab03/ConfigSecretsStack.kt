package io.hellopulumi.lab03

import com.pulumi.Context
import com.pulumi.aws.cloudwatch.LogGroup
import com.pulumi.aws.cloudwatch.LogGroupArgs
import com.pulumi.aws.ssm.Parameter
import com.pulumi.aws.ssm.ParameterArgs
import com.pulumi.aws.ssm.enums.ParameterType
import com.pulumi.core.Output

/**
 * Lab 03: 같은 코드, 다른 스택(dev/prod) — 설정과 시크릿으로 환경 차이를 표현합니다.
 */
object ConfigSecretsStack {

    fun define(ctx: Context, settings: LabSettings, dbPassword: Output<String>) {
        val prefix = "/hello-pulumi/${settings.environment}"
        val tags = mapOf(
            "Environment" to settings.environment,
            "Owner" to settings.owner,
            "ManagedBy" to "pulumi",
        )

        ctx.log().info("환경=${settings.environment}, 로그 보존=${settings.logRetentionDays}일")

        val logGroup = LogGroup(
            "lab03-app-logs",
            LogGroupArgs.builder()
                .name("$prefix/app")
                .retentionInDays(settings.logRetentionDays)
                .tags(tags)
                .build()
        )

        // 시크릿 Output 을 입력으로 넘기면 Pulumi 상태(state) 파일에도 암호화되어 저장됩니다.
        val parameter = Parameter(
            "lab03-db-password",
            ParameterArgs.builder()
                .name("$prefix/db-password")
                .type(ParameterType.SecureString)
                .value(dbPassword)
                .description("Pulumi secret 에서 주입된 DB 비밀번호 (${settings.environment})")
                .tags(tags)
                .build()
        )

        ctx.export("logGroupName", logGroup.name())
        ctx.export("parameterName", parameter.name())
        ctx.export("isProduction", Output.of(settings.isProduction))
        // 시크릿은 export 해도 CLI 에서 [secret] 으로 가려집니다. (`--show-secrets` 로만 확인 가능)
        ctx.export("dbPassword", dbPassword)
    }
}
