package com.bluecone.app.infra.integration.service;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.infra.integration.channel.IntegrationChannel;
import com.bluecone.app.infra.integration.config.IntegrationProperties;
import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;
import com.bluecone.app.infra.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 集成任务分发核心：事件入口 + 调度发送。
 */
@Service
public class IntegrationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationDispatchService.class);

    private final IntegrationSubscriptionService subscriptionService;
    private final IntegrationDeliveryService deliveryService;
    private final Map<IntegrationChannelType, IntegrationChannel> channelRegistry;
    private final IntegrationProperties properties;
    private final ObjectMapper objectMapper;

    public IntegrationDispatchService(final IntegrationSubscriptionService subscriptionService,
                                      final IntegrationDeliveryService deliveryService,
                                      final Map<IntegrationChannelType, IntegrationChannel> channelRegistry,
                                      final IntegrationProperties properties,
                                      final ObjectMapper objectMapper) {
        this.subscriptionService = Objects.requireNonNull(subscriptionService, "subscriptionService must not be null");
        this.deliveryService = Objects.requireNonNull(deliveryService, "deliveryService must not be null");
        this.channelRegistry = Objects.requireNonNull(channelRegistry, "channelRegistry must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * 事件入口：将 DomainEvent 转为投递任务。
     */
    public void onDomainEvent(final DomainEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Long tenantId = event.getTenantId() == null ? 0L : event.getTenantId();
        String previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(String.valueOf(tenantId));
            List<IntegrationSubscriptionEntity> subscriptions = subscriptionService.findSubscriptions(tenantId, event.getEventType());
            if (subscriptions.isEmpty()) {
                log.debug("[Integration] no subscription for eventType={} tenant={}", event.getEventType(), tenantId);
                return;
            }
            String payload = objectMapper.writeValueAsString(event);
            String headers = objectMapper.writeValueAsString(event.getMetadata().getAttributes());
            for (IntegrationSubscriptionEntity subscription : subscriptions) {
                deliveryService.createDelivery(event, subscription, payload, headers);
            }
        } catch (Exception ex) {
            log.error("[Integration] failed to enqueue eventId={} type={} tenant={} error={}",
                    event.getEventId(), event.getEventType(), tenantId, ex.getMessage(), ex);
        } finally {
            restoreTenant(previousTenant);
        }
    }

    /**
     * 调度入口：扫描 due 的 delivery 并分发到各通道。
     */
    public void dispatchDueDeliveries() {
        List<IntegrationDeliveryEntity> deliveries = deliveryService.findDueDeliveries(properties.getDispatchBatchSize());
        if (deliveries.isEmpty()) {
            return;
        }
        int successCount = 0;
        int failureCount = 0;
        log.info("[Integration][Dispatch] start batch size={}", deliveries.size());
        for (IntegrationDeliveryEntity delivery : deliveries) {
            String previousTenant = TenantContext.getTenantId();
            try {
                if (delivery.getTenantId() != null) {
                    TenantContext.setTenantId(String.valueOf(delivery.getTenantId()));
                }
                if (!deliveryService.markSending(delivery.getId())) {
                    continue;
                }
                IntegrationSubscriptionEntity subscription = subscriptionService.findById(delivery.getSubscriptionId(), delivery.getTenantId());
                if (subscription == null || Boolean.FALSE.equals(subscription.getEnabled())) {
                    deliveryService.markDead(delivery, "subscription missing or disabled");
                    failureCount++;
                    continue;
                }
                IntegrationChannel channel = channelRegistry.get(delivery.getChannelType());
                if (channel == null) {
                    deliveryService.markDead(delivery, "channel not found: " + delivery.getChannelType());
                    failureCount++;
                    continue;
                }
                String traceId = extractTraceId(delivery.getHeaders());
                if (traceId != null) {
                    MDC.put("traceId", traceId);
                }
                IntegrationDeliveryResult result = channel.send(delivery, subscription);
                if (result.isSuccess()) {
                    deliveryService.markSuccess(delivery, result);
                    successCount++;
                } else {
                    deliveryService.markFailed(delivery, result, resolveMaxRetry(subscription));
                    failureCount++;
                }
            } catch (Exception ex) {
                failureCount++;
                IntegrationDeliveryResult result = IntegrationDeliveryResult.failure("DISPATCH_EXCEPTION", ex.getMessage(), null, 0L);
                deliveryService.markFailed(delivery, result, resolveMaxRetry(null));
                log.error("[Integration][Dispatch] exception deliveryId={} eventId={} error={}",
                        delivery.getId(), delivery.getEventId(), ex.getMessage(), ex);
            } finally {
                MDC.clear();
                restoreTenant(previousTenant);
            }
        }
        log.info("[Integration][Dispatch] finished batch size={} success={} failure={}", deliveries.size(), successCount, failureCount);
    }

    private int resolveMaxRetry(final IntegrationSubscriptionEntity subscription) {
        if (subscription != null && subscription.getMaxRetry() != null) {
            return subscription.getMaxRetry();
        }
        return properties.getDefaultMaxRetry();
    }

    private String extractTraceId(final String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return null;
        }
        try {
            Map<?, ?> headers = objectMapper.readValue(headersJson, Map.class);
            Object traceId = headers.get("traceId");
            return traceId == null ? null : traceId.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    private void restoreTenant(final String previousTenant) {
        if (previousTenant == null) {
            TenantContext.clear();
        } else {
            TenantContext.setTenantId(previousTenant);
        }
    }
}
