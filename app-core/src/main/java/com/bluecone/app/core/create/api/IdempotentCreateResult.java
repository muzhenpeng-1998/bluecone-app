package com.bluecone.app.core.create.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 幂等创建结果。
 *
 * @param replayed   是否为重放历史结果
 * @param inProgress 是否仍在处理中（通常在 waitForCompletion=false 时返回）
 * @param publicId   对外 ID（public_id）
 * @param internalId 内部 ULID 128 位表示
 * @param value      业务返回值
 * @param <T>        返回类型
 */
public record IdempotentCreateResult<T>(
        boolean replayed,
        boolean inProgress,
        String publicId,
        Ulid128 internalId,
        T value
) {
}

