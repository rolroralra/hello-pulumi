rootProject.name = "hello-pulumi"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

// 각 실습(lab)은 독립된 Pulumi 프로젝트(Pulumi.yaml 보유)이자 Gradle 서브프로젝트입니다.
// Pulumi Java 런타임은 lab 디렉터리에서 상위로 settings.gradle.kts 를 찾아
// `./gradlew :labs:<lab>:run` 형태로 프로그램을 실행합니다.
include(
    ":test-support",
    ":labs:01-s3-bucket",
    ":labs:02-static-website",
    ":labs:03-config-secrets",
    ":labs:04-vpc-ec2",
    ":labs:05-component-resource",
    ":labs:06-stack-reference",
)
