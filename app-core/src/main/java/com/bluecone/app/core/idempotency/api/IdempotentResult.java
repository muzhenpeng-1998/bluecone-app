package com.bluecone.app.core.idempotency.api;

/**
 * 幂等执行结果，标识本次执行是否为重放、是否仍在处理中以及业务返回值。
 *
 * @param replayed    是否为历史结果重放（true 表示未执行 supplier）
 * @param inProgress  是否存在正在执行中的并发请求（通常在 waitForCompletion=false 时返回）
 * @param value       业务返回值（仅在 SUCCEEDED 或重放成功时非空）
 * @param <T>         返回值类型
 */
public record IdempotentResult<T>(
        boolean replayed,
        boolean inProgress,
        T value
) {
}

