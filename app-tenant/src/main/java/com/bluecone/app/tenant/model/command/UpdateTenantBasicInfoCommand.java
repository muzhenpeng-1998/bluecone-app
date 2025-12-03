package com.bluecone.app.tenant.model.command;

/**
 * 更新租户基础信息命令。
 * - 聚焦 tenant 主表字段（名称、联系人、状态等）
 * - 审计由应用服务统一写入
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
