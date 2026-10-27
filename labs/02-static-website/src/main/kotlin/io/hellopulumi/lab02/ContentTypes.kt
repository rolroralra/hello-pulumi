package io.hellopulumi.lab02

import java.nio.file.Path

/** 확장자로 S3 객체의 Content-Type 을 결정합니다. */
object ContentTypes {

    private const val DEFAULT = "application/octet-stream"

    private val byExtension = mapOf(
        "html" to "text/html; charset=utf-8",
        "css" to "text/css; charset=utf-8",
        "js" to "application/javascript",
        "json" to "application/json",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "txt" to "text/plain; charset=utf-8",
    )

    fun of(path: Path): String {
        val extension = path.fileName.toString().substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return byExtension[extension] ?: DEFAULT
    }
}
