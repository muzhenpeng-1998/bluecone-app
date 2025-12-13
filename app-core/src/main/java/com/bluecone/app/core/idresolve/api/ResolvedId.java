package com.bluecone.app.core.idresolve.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 同时携带 publicId 与内部 ULID128 的解析结果，用于 Controller 注入。
 *
 * @param internalId 内部 ULID128
 * @param publicId   原始 publicId
 */
public record ResolvedId(Ulid128 internalId, String publicId) {
}

