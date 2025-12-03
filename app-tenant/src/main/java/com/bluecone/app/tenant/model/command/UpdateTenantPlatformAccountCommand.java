package com.bluecone.app.tenant.model.command;

import java.time.LocalDateTime;

/**
 * 绑定/更新租户的第三方平台账号命令。
 */
public record UpdateTenantPlatformAccountCommand(
        Long tenantId,
        String platformType,
        String platformAccountId,
        String accountName,
        String credential,
        Byte status,
        LocalDateTime expireAt,
        Long operatorId) {
}
