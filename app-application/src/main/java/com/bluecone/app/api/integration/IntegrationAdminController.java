package com.bluecone.app.api.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.infra.integration.domain.IntegrationDeliveryStatus;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.mapper.IntegrationDeliveryMapper;
import com.bluecone.app.infra.integration.mapper.IntegrationSubscriptionMapper;
import com.bluecone.app.infra.integration.service.IntegrationDeliveryService;
import com.bluecone.app.infra.integration.service.IntegrationDispatchService;
import com.bluecone.app.infra.integration.service.IntegrationSubscriptionService;
import com.bluecone.app.core.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;

/**
 * Integration Hub 管理接口（后台使用）。
 *
 * <p>TODO：实际环境需接入 ADMIN 鉴权。</p>
 */
@RestController
@RequestMapping("/api/admin/integration")
public class IntegrationAdminController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationAdminController.class);

    private final IntegrationSubscriptionMapper subscriptionMapper;
    private final IntegrationDeliveryMapper deliveryMapper;
    private final IntegrationDispatchService dispatchService;
    private final IntegrationDeliveryService deliveryService;
    private final IntegrationSubscriptionService subscriptionService;

    public IntegrationAdminController(final IntegrationSubscriptionMapper subscriptionMapper,
                                      final IntegrationDeliveryMapper deliveryMapper,
                                      final IntegrationDispatchService dispatchService,
                                      final IntegrationDeliveryService deliveryService,
                                      final IntegrationSubscriptionService subscriptionService) {
        this.subscriptionMapper = Objects.requireNonNull(subscriptionMapper, "subscriptionMapper must not be null");
        this.deliveryMapper = Objects.requireNonNull(deliveryMapper, "deliveryMapper must not be null");
        this.dispatchService = Objects.requireNonNull(dispatchService, "dispatchService must not be null");
        this.deliveryService = Objects.requireNonNull(deliveryService, "deliveryService must not be null");
        this.subscriptionService = Objects.requireNonNull(subscriptionService, "subscriptionService must not be null");
    }

    @GetMapping("/subscriptions")
    public Page<IntegrationSubscriptionEntity> listSubscriptions(@RequestParam(defaultValue = "1") int page,
                                                                 @RequestParam(defaultValue = "20") int size,
                                                                 @RequestParam(required = false) Long tenantId,
                                                                 @RequestParam(required = false) String eventType) {
        Page<IntegrationSubscriptionEntity> pager = new Page<>(page, size);
        LambdaQueryWrapper<IntegrationSubscriptionEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(IntegrationSubscriptionEntity::getTenantId, tenantId);
        }
        if (eventType != null) {
            wrapper.eq(IntegrationSubscriptionEntity::getEventType, eventType);
        }
        wrapper.orderByDesc(IntegrationSubscriptionEntity::getUpdatedAt);
        return subscriptionMapper.selectPage(pager, wrapper);
    }

    @PostMapping("/subscriptions")
    public IntegrationSubscriptionEntity upsertSubscription(@RequestBody IntegrationSubscriptionEntity request) {
        Objects.requireNonNull(request.getTenantId(), "tenantId required");
        String previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(String.valueOf(request.getTenantId()));
            if (request.getId() == null) {
                subscriptionMapper.insert(request);
                log.info("[Integration][Admin] create subscription tenant={} eventType={} channel={} id={}",
                        request.getTenantId(), request.getEventType(), request.getChannelType(), request.getId());
            } else {
                subscriptionMapper.updateById(request);
                log.info("[Integration][Admin] update subscription id={} tenant={} eventType={}",
                        request.getId(), request.getTenantId(), request.getEventType());
                subscriptionService.refreshCacheForTenant(request.getTenantId());
            }
            return request;
        } finally {
            restoreTenant(previousTenant);
        }
    }

    @GetMapping("/deliveries")
    public Page<IntegrationDeliveryEntity> listDeliveries(@RequestParam(defaultValue = "1") int page,
                                                          @RequestParam(defaultValue = "20") int size,
                                                          @RequestParam(required = false) Long tenantId,
                                                          @RequestParam(required = false) String eventType,
                                                          @RequestParam(required = false) IntegrationDeliveryStatus status) {
        Page<IntegrationDeliveryEntity> pager = new Page<>(page, size);
        LambdaQueryWrapper<IntegrationDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(IntegrationDeliveryEntity::getTenantId, tenantId);
        }
        if (eventType != null) {
            wrapper.eq(IntegrationDeliveryEntity::getEventType, eventType);
        }
        if (status != null) {
            wrapper.eq(IntegrationDeliveryEntity::getStatus, status);
        }
        wrapper.orderByDesc(IntegrationDeliveryEntity::getCreatedAt);
        return deliveryMapper.selectPage(pager, wrapper);
    }

    @PostMapping("/deliveries/{id}/retry")
    public Map<String, Object> retryDelivery(@PathVariable Long id) {
        boolean reset = deliveryService.resetToNew(id);
        if (reset) {
            dispatchService.dispatchDueDeliveries();
        }
        return Map.of("reset", reset, "deliveryId", id);
    }

    @PostMapping("/test")
    public Map<String, Object> testSend(@RequestBody TestSendRequest request) {
        IntegrationSubscriptionEntity subscription = subscriptionService.findById(request.getSubscriptionId(), request.getTenantId());
        if (subscription == null) {
            return Map.of("success", false, "message", "subscription not found");
        }
        String previousTenant = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(String.valueOf(subscription.getTenantId()));
            DomainEvent event = new TestIntegrationEvent(subscription.getEventType(), subscription.getTenantId(), request.getPayload());
            dispatchService.onDomainEvent(event);
            dispatchService.dispatchDueDeliveries();
            return Map.of("success", true, "message", "test event enqueued");
        } finally {
            restoreTenant(previousTenant);
        }
    }

    private void restoreTenant(final String previousTenant) {
        if (previousTenant == null) {
            TenantContext.clear();
        } else {
            TenantContext.setTenantId(previousTenant);
        }
    }

    public static class TestSendRequest {
        private Long subscriptionId;
        private Long tenantId;
        private Map<String, Object> payload;

        public Long getSubscriptionId() {
            return subscriptionId;
        }

        public void setSubscriptionId(final Long subscriptionId) {
            this.subscriptionId = subscriptionId;
        }

        public Long getTenantId() {
            return tenantId;
        }

        public void setTenantId(final Long tenantId) {
            this.tenantId = tenantId;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }

        public void setPayload(final Map<String, Object> payload) {
            this.payload = payload;
        }
    }

    /**
     * 简单测试事件载体，仅用于管理端手工触发。
     */
    public static class TestIntegrationEvent extends DomainEvent {

        private final Map<String, Object> payload;

        public TestIntegrationEvent(final String eventType, final Long tenantId, final Map<String, Object> payload) {
            super(eventType, EventMetadata.of(Map.of(
                    "tenantId", tenantId == null ? "0" : tenantId.toString(),
                    "traceId", "integration-test",
                    "source", "integration-admin"
            )));
            this.payload = payload;
        }

        public Map<String, Object> getPayload() {
            return payload;
        }
    }
}
