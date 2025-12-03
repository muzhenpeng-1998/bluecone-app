package com.bluecone.app.tenant.model.command;

/**
 * 更新租户主体/认证资料命令。
 * - 涵盖主体类型、营业执照、法人信息、认证状态/驳回原因
 * - 可重复调用，支持补录与重提
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
