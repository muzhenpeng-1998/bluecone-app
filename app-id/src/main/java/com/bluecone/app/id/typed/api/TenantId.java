package com.bluecone.app.id.typed.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 租户 ID 强类型包装。
 */
public record TenantId(Ulid128 internal) implements TypedId {

    @Override
    public String type() {
        return "ten";
    }
}

