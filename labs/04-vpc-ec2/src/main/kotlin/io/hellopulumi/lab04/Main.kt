package io.hellopulumi.lab04

import com.pulumi.Pulumi

fun main() {
    Pulumi.run { ctx ->
        val config = ctx.config()
        val settings = NetworkSettings.from { key -> config.get(key).orElse(null) }
        VpcEc2Stack.define(ctx, settings)
    }
}
