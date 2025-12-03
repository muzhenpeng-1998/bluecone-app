package com.bluecone.app.tenant.model.command;

/**
 * 更新租户基础信息命令。
 */
public record UpdateTenantBasicInfoCommand(
        Long tenantId,
        String tenantName,
        String contactPerson,
        String contactPhone,
        String contactEmail,
        String remark,
        Integer status,
        Long operatorId) {
}
