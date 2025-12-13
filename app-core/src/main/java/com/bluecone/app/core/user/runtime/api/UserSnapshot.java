package com.bluecone.app.core.user.runtime.api;

import com.bluecone.app.id.core.Ulid128;

import java.time.Instant;
import java.util.Map;

/**
 * 用户运行态快照（轻量级），用于网关/业务在进入核心逻辑前快速判断用户状态。
 */
public record UserSnapshot(
        long tenantId,
        Ulid128 userId,
        int status,
        boolean phoneBound,
        String memberLevel,
        long configVersion,
        Instant updatedAt,
        Map<String, Object> ext
) {
}

