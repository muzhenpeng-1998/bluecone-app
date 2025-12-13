package com.bluecone.app.core.idresolve.api;

import com.bluecone.app.id.core.Ulid128;

/**
 * 公共 ID 解析结果。
 *
 * @param hit        是否命中解析流程（含缓存/DB/回退表），校验失败则为 false
 * @param exists     publicId 是否存在，对应内部 ID 是否找到
 * @param internalId 内部 ULID128
 * @param publicId   原始 publicId
 * @param reason     结果原因：HIT_L1/HIT_L2/HIT_DB/NOT_FOUND/PREFIX_MISMATCH/INVALID_FORMAT/DISABLED 等
 */
public record ResolveResult(
        boolean hit,
        boolean exists,
        Ulid128 internalId,
        String publicId,
        String reason
) {

    public static final String REASON_INVALID_FORMAT = "INVALID_FORMAT";
    public static final String REASON_PREFIX_MISMATCH = "PREFIX_MISMATCH";
    public static final String REASON_NOT_FOUND = "NOT_FOUND";
    public static final String REASON_DISABLED = "DISABLED";
}

