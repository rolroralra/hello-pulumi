// 모든 lab 이 공유하는 컨벤션 플러그인
plugins {
    id("org.jetbrains.kotlin.jvm")
    application
    id("org.jetbrains.kotlinx.kover")
}

val libs: VersionCatalog = versionCatalogs.named("libs")

fun lib(alias: String) = libs.findLibrary(alias).orElseThrow {
    GradleException("gradle/libs.versions.toml 에 '$alias' 라이브러리가 정의되어 있지 않습니다.")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(lib("pulumi-sdk"))
    implementation(lib("pulumi-aws"))

    testImplementation(platform(lib("junit-bom")))
    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("kotest-assertions"))
    testRuntimeOnly(lib("junit-platform-launcher"))

    // 모든 lab 테스트에서 공용 Pulumi Mock 을 사용합니다.
    testImplementation(project(":test-support"))
}

application {
    // Pulumi CLI 는 플러그인 탐색 시 `-PmainClass=com.pulumi.bootstrap.internal.Main` 을 넘깁니다.
    // 이 프로퍼티가 없으면 lab 의 `labPackage` 아래 Main.kt 를 실행합니다.
    mainClass.set(
        providers.gradleProperty("mainClass")
            .orElse(provider { "${project.property("labPackage")}.MainKt" })
    )
}

tasks.test {
    useJUnitPlatform()
}

// 커버리지: Main.kt(Pulumi.run 진입점)는 실제 Pulumi 엔진이 필요하므로 측정에서 제외합니다.
kover {
    reports {
        filters {
            excludes {
                classes("*.MainKt")
            }
        }
        verify {
            rule {
                minBound(80)
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.named("koverVerify"))
}
