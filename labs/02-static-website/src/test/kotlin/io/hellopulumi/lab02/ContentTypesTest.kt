package io.hellopulumi.lab02

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.nio.file.Path

class ContentTypesTest {

    @ParameterizedTest
    @CsvSource(
        "index.html, text/html; charset=utf-8",
        "STYLE.CSS, text/css; charset=utf-8",
        "app.js, application/javascript",
        "logo.svg, image/svg+xml",
        "README, application/octet-stream",
        "archive.tar.gz, application/octet-stream",
    )
    fun `확장자로 Content-Type 을 결정한다`(fileName: String, expected: String) {
        ContentTypes.of(Path.of(fileName)) shouldBe expected
    }
}
