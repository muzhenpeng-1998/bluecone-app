package com.bluecone.app.ops.api.dto.drill;

public record IdemConflictItem(
        long id,
        long tenantId,
        String bizType,
        String idemKeyHashPrefix,
        String requestHashPrefix,
        String status,
        String errorCode,
        String errorMsg,
        String createdAt
) {
}

