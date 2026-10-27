package io.hellopulumi.lab03

import com.pulumi.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val config = ctx.config()
        val settings = LabSettings.from { key -> config.get(key).orElse(null) }
        val dbPassword = config.requireSecret("dbPassword")
        ConfigSecretsStack.define(ctx, settings, dbPassword)
    }
}
