package com.bluecone.app.tenant.application.onboarding;

/**
 * 入驻引导创建首店草稿命令。
 * <p>仅用于 H5 入驻引导流程，不负责完整门店配置。</p>
 */
public record CreateStoreDraftCommand(
        // 租户 ID
        Long tenantId,
        // 门店名称
        String storeName,
        // 城市编码或名称
        String city,
        // 区县编码或名称
        String district,
        // 详细地址
        String address,
        // 经营场景：COFFEE / FOOD / BAKERY 等
        String bizScene,
        // 门店联系电话（可复用入驻人手机号）
        String contactPhone) {
}

