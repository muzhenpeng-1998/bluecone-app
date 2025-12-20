package com.bluecone.app.infra.integration.channel;

import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;
import com.bluecone.app.infra.notify.channel.wechat.WeChatBotClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 复用通知平台的 WeChatBotClient 作为 Integration 通道。
 */
@Component
public class WeChatBotChannelAdapter implements IntegrationChannel {

    private static final Logger log = LoggerFactory.getLogger(WeChatBotChannelAdapter.class);

    private final WeChatBotClient weChatBotClient;
    private final ObjectMapper objectMapper;

    public WeChatBotChannelAdapter(final WeChatBotClient weChatBotClient,
                                   @Qualifier("redisObjectMapper") final ObjectMapper objectMapper) {
        this.weChatBotClient = Objects.requireNonNull(weChatBotClient, "weChatBotClient must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public IntegrationChannelType type() {
        return IntegrationChannelType.WECHAT_BOT;
    }

    @Override
    public IntegrationDeliveryResult send(final IntegrationDeliveryEntity delivery,
                                          final IntegrationSubscriptionEntity subscription) {
        long start = System.currentTimeMillis();
        if (subscription.getTargetUrl() == null || subscription.getTargetUrl().isBlank()) {
            return IntegrationDeliveryResult.failure("TARGET_URL_MISSING", "wechat webhook is blank", null, 0L);
        }
        try {
            String content = buildMarkdown(delivery);
            log.info("[Integration][WeChatBot] send eventId={} subscription={} tenant={}",
                    delivery.getEventId(), subscription.getId(), delivery.getTenantId());
            weChatBotClient.sendMarkdown(subscription.getTargetUrl(), content, null, false);
            long duration = System.currentTimeMillis() - start;
            return IntegrationDeliveryResult.success(200, duration);
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - start;
            log.warn("[Integration][WeChatBot] send failed eventId={} subscription={} tenant={} error={}",
                    delivery.getEventId(), subscription.getId(), delivery.getTenantId(), ex.getMessage());
            return IntegrationDeliveryResult.failure("WECHAT_SEND_ERROR", ex.getMessage(), null, duration);
        }
    }

    private String buildMarkdown(final IntegrationDeliveryEntity delivery) {
        try {
            Object payload = objectMapper.readTree(delivery.getPayload());
            return "### Integration Event\n" +
                    "- Event: **" + delivery.getEventType() + "**\n" +
                    "- Tenant: " + delivery.getTenantId() + "\n" +
                    "- EventId: `" + delivery.getEventId() + "`\n" +
                    "\n" +
                    "```json\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload) + "\n```";
        } catch (Exception ex) {
            return "Integration Event " + delivery.getEventType() + " payload: " + delivery.getPayload();
        }
    }
}
