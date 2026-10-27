package io.hellopulumi.lab05

import com.pulumi.Context
import com.pulumi.Pulumi

/** 같은 컴포넌트로 두 개의 사이트를 만듭니다. */
fun defineSites(ctx: Context) {
    val tags = mapOf("Project" to ctx.projectName(), "Stack" to ctx.stackName())

    val docs = StaticWebsite(
        "docs-site",
        StaticWebsiteArgs(indexHtml = "<h1>Docs</h1><p>Pulumi ComponentResource 로 만든 문서 사이트</p>", tags = tags),
    )
    val blog = StaticWebsite(
        "blog-site",
        StaticWebsiteArgs(indexHtml = "<h1>Blog</h1><p>같은 컴포넌트, 다른 인스턴스</p>", tags = tags),
    )

    ctx.export("docsUrl", docs.websiteUrl)
    ctx.export("blogUrl", blog.websiteUrl)
}

fun main() {
    Pulumi.run(::defineSites)
}
