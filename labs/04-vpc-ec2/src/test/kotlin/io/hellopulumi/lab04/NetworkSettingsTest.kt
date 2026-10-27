package io.hellopulumi.lab04

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class NetworkSettingsTest {

    @Test
    fun `설정이 없으면 기본값을 사용한다`() {
        NetworkSettings.from { null } shouldBe NetworkSettings()
    }

    @Test
    fun `설정값을 읽는다`() {
        val settings = NetworkSettings.from(
            mapOf("instanceType" to "t3.small", "allowedHttpCidr" to "203.0.113.7/32")::get
        )

        settings.instanceType shouldBe "t3.small"
        settings.allowedHttpCidr shouldBe "203.0.113.7/32"
    }

    @ParameterizedTest
    @ValueSource(strings = ["10.0.0.0", "10.0.0.0/33", "256.0.0.0/16", "abc"])
    fun `잘못된 CIDR 은 거부한다`(cidr: String) {
        val error = shouldThrow<IllegalArgumentException> { NetworkSettings(allowedHttpCidr = cidr) }

        error.message shouldContain "allowedHttpCidr"
    }

    @Test
    fun `인스턴스 타입은 비어 있을 수 없다`() {
        shouldThrow<IllegalArgumentException> { NetworkSettings(instanceType = "") }
    }
}
