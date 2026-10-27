package io.hellopulumi.lab06

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class AppSettingsTest {

    @Test
    fun `설정을 읽고 포트 기본값을 사용한다`() {
        AppSettings.from(mapOf("networkStack" to "organization/lab04-vpc-ec2/dev")::get) shouldBe
            AppSettings("organization/lab04-vpc-ec2/dev", 8080)
    }

    @Test
    fun `networkStack 이 없으면 설정 방법을 안내한다`() {
        val error = shouldThrow<IllegalArgumentException> { AppSettings.from { null } }

        error.message shouldContain "pulumi config set networkStack"
    }

    @Test
    fun `스택 이름 형식이 틀리면 거부한다`() {
        shouldThrow<IllegalArgumentException> { AppSettings("lab04-vpc-ec2") }
    }

    @Test
    fun `포트 범위를 검증한다`() {
        shouldThrow<IllegalArgumentException> { AppSettings("o/p/s", 70000) }
        shouldThrow<IllegalArgumentException> {
            AppSettings.from(mapOf("networkStack" to "o/p/s", "appPort" to "http")::get)
        }
    }
}
