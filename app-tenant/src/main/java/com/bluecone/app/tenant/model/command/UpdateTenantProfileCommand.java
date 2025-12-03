package com.bluecone.app.tenant.model.command;

/**
 * 更新租户主体资料命令。
 */
public record UpdateTenantProfileCommand(
        Long tenantId,
        Byte tenantType,
        String businessName,
        String businessLicenseNo,
        String businessLicenseUrl,
        String legalPersonName,
        String legalPersonIdNo,
        String address,
        Byte verificationStatus,
        String verificationReason,
        Long operatorId) {
}
