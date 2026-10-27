// lab 단위 테스트에서 공유하는 Pulumi Mock 유틸리티 (Pulumi 프로젝트 아님)
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.pulumi.sdk)
}
