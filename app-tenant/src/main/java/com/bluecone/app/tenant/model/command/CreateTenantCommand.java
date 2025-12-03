package com.bluecone.app.tenant.model.command;

import java.time.LocalDateTime;

/**
 * 创建租户聚合命令。
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
