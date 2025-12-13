package com.bluecone.app.product.runtime.api;

import com.bluecone.app.id.core.Ulid128;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * SKU 轻量快照，覆盖下单确认/提交订单所需字段。
 */
public record SkuSnapshot(
        Ulid128 skuId,
        String skuPublicId,
        Ulid128 productId,
        long tenantId,
        int status,
        long configVersion,
        String skuName,
        BigDecimal price,
        String currency,
        Instant updatedAt,
        Map<String, Object> ext
) {
}

