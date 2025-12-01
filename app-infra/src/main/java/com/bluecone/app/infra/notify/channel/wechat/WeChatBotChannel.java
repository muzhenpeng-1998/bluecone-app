package com.bluecone.app.infra.notify.channel.wechat;

import com.bluecone.app.infra.notify.config.NotifyProperties;
import com.bluecone.app.infra.notify.delivery.DeliveryResult;
import com.bluecone.app.infra.notify.delivery.NotificationChannel;
import com.bluecone.app.infra.notify.delivery.NotificationEnvelope;
import com.bluecone.app.infra.notify.policy.NotifyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * 微信机器人通道插件（Delivery 层）。
 */
public class WeChatBotChannel implements NotificationChannel {

    private static final Logger log = LoggerFactory.getLogger(WeChatBotChannel.class);

    private final WeChatBotProperties properties;
    private final WeChatBotClient client;
    private final WeChatBotMessageBuilder messageBuilder;
    private final NotifyProperties notifyProperties;

    public WeChatBotChannel(WeChatBotProperties properties,
                            WeChatBotClient client,
                            WeChatBotMessageBuilder messageBuilder,
                            NotifyProperties notifyProperties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.messageBuilder = Objects.requireNonNull(messageBuilder, "messageBuilder must not be null");
        this.notifyProperties = Objects.requireNonNull(notifyProperties, "notifyProperties must not be null");
    }

    @Override
    public NotifyChannel channel() {
        return NotifyChannel.WECHAT_BOT;
    }

    @Override
    public DeliveryResult deliver(NotificationEnvelope envelope) {
        WeChatBotProperties.WeChatBotWebhook webhook = resolveWebhook(envelope);
        if (webhook == null) {
            return DeliveryResult.failure("WEBHOOK_NOT_FOUND", "No webhook configured for tenant");
        }
        String content = messageBuilder.buildMarkdown(envelope, envelope.getTask().getTemplateCode());
        log.info("[WeChatBot] send traceId={} tenant={} scenario={} channel={} webhook={}",
                envelope.getIntent().getTraceId(),
                envelope.getIntent().getTenantId(),
                envelope.getIntent().getScenarioCode(),
                channel(),
                webhook.getName());
        if (notifyProperties.isDebugMode()) {
            log.info("[WeChatBot][Debug] skip real send content={}", content);
            return DeliveryResult.success();
        }
        try {
            client.sendMarkdown(webhook.getWebhookUrl(), content, webhook.getMentionMobiles(), webhook.isMentionAll());
            return DeliveryResult.success();
        } catch (Exception ex) {
            log.error("[WeChatBot] send failed tenant={} scenario={} webhook={}", envelope.getIntent().getTenantId(),
                    envelope.getIntent().getScenarioCode(), webhook.getName(), ex);
            return DeliveryResult.failure("SEND_ERROR", ex.getMessage());
        }
    }

    private WeChatBotProperties.WeChatBotWebhook resolveWebhook(NotificationEnvelope envelope) {
        Long tenantId = envelope.getIntent().getTenantId();
        WeChatBotProperties.WeChatBotTenantConfig tenantConfig = properties.getTenants().get(tenantId);
        if (tenantConfig == null) {
            tenantConfig = properties.getTenants().get(null);
        }
        if (tenantConfig == null || tenantConfig.getBots() == null || tenantConfig.getBots().isEmpty()) {
            return null;
        }
        if (envelope.getTask().getChannelConfigId() != null) {
            for (WeChatBotProperties.WeChatBotWebhook bot : tenantConfig.getBots()) {
                if (envelope.getTask().getChannelConfigId().equals(bot.getId())) {
                    return bot;
                }
            }
        }
        List<WeChatBotProperties.WeChatBotWebhook> bots = tenantConfig.getBots();
        return bots.get(0);
    }
}
