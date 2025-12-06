package com.bluecone.app.payment.domain.channel;

import java.util.Optional;

/**
 * 支付渠道配置仓储接口。
 */
public interface PaymentChannelConfigRepository {

    /**
     * 按租户/门店/渠道查询配置。
     */
    Optional<PaymentChannelConfig> findByTenantStoreAndChannel(Long tenantId, Long storeId, PaymentChannelType channelType);

    /**
     * 新建配置。
     */
    void save(PaymentChannelConfig config);

    /**
     * 更新配置。
     */
    void update(PaymentChannelConfig config);
}
