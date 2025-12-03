package com.bluecone.app.tenant.model.command;

import java.time.LocalDateTime;

/**
 * 创建租户聚合命令。
 * - 只携带创建场景需要的业务字段，不暴露数据库细节
 * - initialPlanId/planExpireAt 允许空，表示创建后先挂免费版
 * - operatorId 预留审计来源，未来可由安全模块注入
 */
public record CreateTenantCommand(
        String tenantName,
        String contactPerson,
        String contactPhone,
        String contactEmail,
        String remark,
        Byte tenantType,
        String businessName,
        String businessLicenseNo,
        String businessLicenseUrl,
        String legalPersonName,
        String legalPersonIdNo,
        String address,
        Long operatorId,
        Long initialPlanId,
        LocalDateTime planExpireAt) {
}
