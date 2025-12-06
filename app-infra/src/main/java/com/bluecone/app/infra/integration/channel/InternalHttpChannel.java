package com.bluecone.app.infra.integration.channel;

import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 预留内部 HTTP 通道，目前简单回显，后续可接入内网网关。
 */
@Component
public class InternalHttpChannel implements IntegrationChannel {

    private static final Logger log = LoggerFactory.getLogger(InternalHttpChannel.class);

    @Override
    public IntegrationChannelType type() {
        return IntegrationChannelType.INTERNAL_HTTP;
    }

    @Override
    public IntegrationDeliveryResult send(final IntegrationDeliveryEntity delivery,
                                          final IntegrationSubscriptionEntity subscription) {
        log.info("[Integration][InternalHttp] stub send eventId={} subscription={}", delivery.getEventId(), subscription.getId());
        return IntegrationDeliveryResult.success(200, 0L);
    }
}
