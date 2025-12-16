package com.bluecone.app.id.publicid.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 解析后的公开 ID，包含业务类型与内部 128 位 ULID 表示。
 *
 * @param type 业务类型/前缀
 * @param id   内部 ULID 128 位表示
 */
public record DecodedPublicId(String type, Ulid128 id) {
}
