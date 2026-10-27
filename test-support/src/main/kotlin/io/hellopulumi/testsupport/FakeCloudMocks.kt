package io.hellopulumi.testsupport

import com.pulumi.test.Mocks
import java.util.Optional
import java.util.concurrent.CompletableFuture

/** 리소스 타입별로 "클라우드가 계산해 주는 값"(arn, publicIp 등)을 흉내 내는 함수 */
typealias ComputedState = (Mocks.ResourceArgs) -> Map<String, Any>

/**
 * 실제 AWS 호출 없이 Pulumi 프로그램을 실행하기 위한 Mock.
 *
 * - 리소스 생성: 입력값(inputs)을 그대로 상태로 돌려주고, [computed] 에 정의된 출력값을 덧붙입니다.
 * - 함수 호출(invoke, 예: aws:ssm/getParameter): [calls] 에 등록된 응답을 반환합니다.
 */
class FakeCloudMocks(
    private val computed: Map<String, ComputedState> = emptyMap(),
    private val calls: Map<String, Map<String, Any>> = emptyMap(),
) : Mocks {

    override fun newResourceAsync(args: Mocks.ResourceArgs): CompletableFuture<Mocks.ResourceResult> {
        val extra = computed[args.type]?.invoke(args).orEmpty()
        val state = args.inputs.orEmpty() + extra
        return CompletableFuture.completedFuture(
            Mocks.ResourceResult.of(Optional.of("${args.name}-id"), state)
        )
    }

    override fun callAsync(args: Mocks.CallArgs): CompletableFuture<Map<String, Any>> {
        val response = calls[args.token]
            ?: return CompletableFuture.failedFuture(
                IllegalStateException("등록되지 않은 invoke 호출입니다: ${args.token}")
            )
        return CompletableFuture.completedFuture(response)
    }
}
