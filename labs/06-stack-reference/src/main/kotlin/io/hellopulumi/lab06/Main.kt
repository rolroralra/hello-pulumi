package io.hellopulumi.lab06

import com.pulumi.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val config = ctx.config()
        val settings = AppSettings.from { key -> config.get(key).orElse(null) }
        StackReferenceStack.define(ctx, settings)
    }
}
