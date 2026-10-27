package io.hellopulumi.lab02

import com.pulumi.Pulumi
import java.nio.file.Path

fun main() {
    Pulumi.run { ctx -> StaticWebsiteStack.define(ctx, Path.of(StaticWebsiteStack.DEFAULT_SITE_DIR)) }
}
