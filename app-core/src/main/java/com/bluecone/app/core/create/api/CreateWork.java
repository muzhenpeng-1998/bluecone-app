package com.bluecone.app.core.create.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 创建操作回调，接受 internalId 与 publicId，并返回业务结果。
 */
@FunctionalInterface
public interface CreateWork<T> {

    /**
     * 执行具体创建逻辑。
     *
     * @param internalId 内部 ULID
     * @param publicId   对外 public_id
     * @return 业务返回值
     */
    T execute(Ulid128 internalId, String publicId);
}

