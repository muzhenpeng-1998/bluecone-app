package com.bluecone.app.tenant.model.command;

import java.time.LocalDateTime;

/**
 * 绑定/更新租户的第三方平台账号命令。
 * - 按 platformType 做 upsert，防止同平台重复插入
 * - credential 可存放授权码/密文，status 表示绑定或过期状态
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
