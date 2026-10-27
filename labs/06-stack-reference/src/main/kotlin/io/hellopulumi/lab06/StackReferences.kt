package io.hellopulumi.lab06

import com.pulumi.core.Output
import com.pulumi.resources.StackReference

/**
 * StackReference.requireOutput 은 Output<?> 를 돌려주므로 Kotlin 에서는 타입을 좁혀 줘야 합니다.
 * 값이 없거나 문자열이 아니면 배포가 명확한 메시지와 함께 실패합니다.
 */
@Suppress("UNCHECKED_CAST")
fun StackReference.requireString(outputName: String): Output<String> =
    (requireOutput(outputName) as Output<Any?>).applyValue { value ->
        value as? String
            ?: throw IllegalStateException("참조 스택의 '$outputName' 출력이 문자열이 아닙니다: $value")
    }
