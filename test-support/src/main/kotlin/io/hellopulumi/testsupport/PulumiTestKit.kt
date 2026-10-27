package io.hellopulumi.testsupport

import com.pulumi.Context
import com.pulumi.core.Output
import com.pulumi.resources.Resource
import com.pulumi.test.Mocks
import com.pulumi.test.PulumiTest
import com.pulumi.test.TestOptions
import com.pulumi.test.TestResult

/** Mock 환경에서 Pulumi 프로그램을 실행하고, 오류가 있으면 즉시 예외를 던집니다. */
fun runPulumiProgram(
    mocks: Mocks = FakeCloudMocks(),
    projectName: String = "test-project",
    stackName: String = "test",
    program: (Context) -> Unit,
): TestResult {
    val options = TestOptions.builder()
        .projectName(projectName)
        .stackName(stackName)
        .preview(false)
        .build()
    return PulumiTest.withOptions(options)
        .withMocks(mocks)
        .runTest { ctx -> program(ctx) }
        .throwOnError()
}

/** 테스트 결과에서 특정 타입의 리소스만 골라냅니다. */
inline fun <reified T : Resource> TestResult.resourcesOf(): List<T> =
    resources().filterIsInstance<T>()

/** Output 의 실제 값을 동기적으로 꺼냅니다 (테스트 전용). */
fun <T> Output<T>.await(): T =
    requireNotNull(PulumiTest.extractValue(this)) { "Output 값이 null 입니다 (unknown 값일 수 있습니다)." }

/** 스택 export 값을 꺼냅니다. */
fun TestResult.exported(name: String): Any? = output(name).await()
