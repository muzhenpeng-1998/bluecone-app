package com.bluecone.app.tenant.application.onboarding;

/**
 * 入驻引导创建租户草稿命令。
 * <p>仅用于 H5 入驻引导流程，不暴露数据库细节。</p>
 */
public record CreateTenantDraftCommand(
        // 品牌展示名称，例如 "Dont Worry Coffee"
        String tenantName,
        // 主体名称（公司 / 个体）
        String legalName,
        // 业态标识，如 "COFFEE"、"RESTAURANT"
        String businessCategory,
        // 获客/招商渠道代码，可来自入驻会话的 channel_code
        String sourceChannel,
        // 创建人用户ID（入驻人）
        Long ownerUserId,
        // 联系电话
        String contactPhone) {
}

