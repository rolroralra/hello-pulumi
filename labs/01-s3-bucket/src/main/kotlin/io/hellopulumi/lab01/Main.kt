package io.hellopulumi.lab01

import com.pulumi.Pulumi

fun main() {
    Pulumi.run { ctx -> S3BucketStack.define(ctx) }
}
