package io.hellopulumi.lab05

/** [StaticWebsite] 컴포넌트 입력값 */
data class StaticWebsiteArgs(
    val indexHtml: String,
    val errorHtml: String = DEFAULT_ERROR_HTML,
    val tags: Map<String, String> = emptyMap(),
) {
    init {
        require(indexHtml.isNotBlank()) { "indexHtml 은 비어 있을 수 없습니다." }
    }

    companion object {
        const val DEFAULT_ERROR_HTML = "<h1>404 Not Found</h1>"
    }
}
