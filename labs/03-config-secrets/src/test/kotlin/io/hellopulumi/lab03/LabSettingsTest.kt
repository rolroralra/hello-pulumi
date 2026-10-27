package io.hellopulumi.lab03

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class LabSettingsTest {

    @Test
    fun `설정이 없으면 기본값을 사용한다`() {
        val settings = LabSettings.from(mapOf("environment" to "dev")::get)

        settings shouldBe LabSettings("dev", LabSettings.DEFAULT_RETENTION_DAYS, LabSettings.DEFAULT_OWNER)
        settings.isProduction shouldBe false
    }

    @Test
    fun `prod 설정을 읽는다`() {
        val settings = LabSettings.from(
            mapOf("environment" to "prod", "logRetentionDays" to "30", "owner" to "sre-team")::get
        )

        settings shouldBe LabSettings("prod", 30, "sre-team")
        settings.isProduction shouldBe true
    }

    @Test
    fun `environment 가 없으면 설정 방법을 안내하며 실패한다`() {
        val error = shouldThrow<IllegalArgumentException> { LabSettings.from(emptyMap<String, String>()::get) }

        error.message shouldContain "pulumi config set environment"
    }

    @Test
    fun `허용되지 않은 환경 이름은 거부한다`() {
        shouldThrow<IllegalArgumentException> { LabSettings.from(mapOf("environment" to "qa")::get) }
    }

    @Test
    fun `CloudWatch 가 허용하지 않는 보존 기간은 거부한다`() {
        val error = shouldThrow<IllegalArgumentException> {
            LabSettings.from(mapOf("environment" to "dev", "logRetentionDays" to "10")::get)
        }

        error.message shouldContain "logRetentionDays"
    }

    @Test
    fun `보존 기간이 숫자가 아니면 거부한다`() {
        val error = shouldThrow<IllegalArgumentException> {
            LabSettings.from(mapOf("environment" to "dev", "logRetentionDays" to "seven")::get)
        }

        error.message shouldContain "정수"
    }

    @Test
    fun `owner 는 비어 있을 수 없다`() {
        shouldThrow<IllegalArgumentException> { LabSettings("dev", 7, " ") }
    }
}
