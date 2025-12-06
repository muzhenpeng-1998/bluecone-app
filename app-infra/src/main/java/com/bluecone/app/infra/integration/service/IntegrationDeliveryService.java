package com.bluecone.app.infra.integration.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.infra.integration.config.IntegrationProperties;
import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.domain.IntegrationDeliveryStatus;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.mapper.IntegrationDeliveryMapper;
import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;
import com.bluecone.app.infra.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 投递任务持久化与状态流转。
 */
@Service
public class IntegrationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationDeliveryService.class);

    private final IntegrationDeliveryMapper mapper;
    private final IntegrationProperties properties;

    public IntegrationDeliveryService(final IntegrationDeliveryMapper mapper,
                                      final IntegrationProperties properties) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    public IntegrationDeliveryEntity createDelivery(final DomainEvent event,
                                                    final IntegrationSubscriptionEntity subscription,
                                                    final String payloadJson,
                                                    final String headersJson) {
        IntegrationDeliveryEntity entity = new IntegrationDeliveryEntity();
        entity.setSubscriptionId(subscription.getId());
        entity.setTenantId(subscription.getTenantId());
        entity.setEventId(event.getEventId());
        entity.setEventType(event.getEventType());
        entity.setPayload(payloadJson);
        entity.setHeaders(headersJson);
        entity.setChannelType(subscription.getChannelType());
        entity.setStatus(IntegrationDeliveryStatus.NEW);
        entity.setRetryCount(0);
        entity.setNextRetryAt(LocalDateTime.now());
        mapper.insert(entity);
        log.info("[Integration][Delivery] created eventId={} subscriptionId={} deliveryId={} channel={}",
                entity.getEventId(), subscription.getId(), entity.getId(), subscription.getChannelType());
        return entity;
    }

    public List<IntegrationDeliveryEntity> findDueDeliveries(final int limit) {
        LambdaQueryWrapper<IntegrationDeliveryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(IntegrationDeliveryEntity::getStatus, Arrays.asList(IntegrationDeliveryStatus.NEW, IntegrationDeliveryStatus.FAILED))
                .and(w -> w.isNull(IntegrationDeliveryEntity::getNextRetryAt)
                        .or()
                        .le(IntegrationDeliveryEntity::getNextRetryAt, LocalDateTime.now()))
                .orderByAsc(IntegrationDeliveryEntity::getNextRetryAt)
                .last("limit " + limit);
        return mapper.selectList(wrapper);
    }

    public boolean markSending(final Long id) {
        LambdaUpdateWrapper<IntegrationDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IntegrationDeliveryEntity::getId, id)
                .in(IntegrationDeliveryEntity::getStatus, IntegrationDeliveryStatus.NEW, IntegrationDeliveryStatus.FAILED)
                .set(IntegrationDeliveryEntity::getStatus, IntegrationDeliveryStatus.SENDING)
                .set(IntegrationDeliveryEntity::getNextRetryAt, null);
        return mapper.update(null, wrapper) > 0;
    }

    public void markSuccess(final IntegrationDeliveryEntity entity, final IntegrationDeliveryResult result) {
        LambdaUpdateWrapper<IntegrationDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IntegrationDeliveryEntity::getId, entity.getId())
                .set(IntegrationDeliveryEntity::getStatus, IntegrationDeliveryStatus.SUCCESS)
                .set(IntegrationDeliveryEntity::getLastHttpStatus, result.getHttpStatus())
                .set(IntegrationDeliveryEntity::getLastError, null)
                .set(IntegrationDeliveryEntity::getLastDurationMs, result.getDurationMs() == null ? null : result.getDurationMs().intValue())
                .set(IntegrationDeliveryEntity::getNextRetryAt, null);
        mapper.update(null, wrapper);
        entity.setStatus(IntegrationDeliveryStatus.SUCCESS);
        log.info("[Integration][Delivery] success deliveryId={} eventId={} channel={}",
                entity.getId(), entity.getEventId(), entity.getChannelType());
    }

    public void markFailed(final IntegrationDeliveryEntity entity,
                           final IntegrationDeliveryResult result,
                           final int maxRetry) {
        int nextRetryCount = (entity.getRetryCount() == null ? 0 : entity.getRetryCount()) + 1;
        boolean dead = nextRetryCount >= maxRetry;
        LocalDateTime nextRetryAt = dead ? null : LocalDateTime.now().plus(computeBackoff(nextRetryCount));

        LambdaUpdateWrapper<IntegrationDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IntegrationDeliveryEntity::getId, entity.getId())
                .set(IntegrationDeliveryEntity::getStatus, dead ? IntegrationDeliveryStatus.DEAD : IntegrationDeliveryStatus.FAILED)
                .set(IntegrationDeliveryEntity::getRetryCount, nextRetryCount)
                .set(IntegrationDeliveryEntity::getNextRetryAt, nextRetryAt)
                .set(IntegrationDeliveryEntity::getLastError, truncate(result.getErrorMessage()))
                .set(IntegrationDeliveryEntity::getLastHttpStatus, result.getHttpStatus())
                .set(IntegrationDeliveryEntity::getLastDurationMs, result.getDurationMs() == null ? null : result.getDurationMs().intValue());
        mapper.update(null, wrapper);

        entity.setStatus(dead ? IntegrationDeliveryStatus.DEAD : IntegrationDeliveryStatus.FAILED);
        entity.setRetryCount(nextRetryCount);
        entity.setNextRetryAt(nextRetryAt);
        if (dead) {
            log.error("[Integration][Delivery] dead deliveryId={} eventId={} retryCount={} error={}",
                    entity.getId(), entity.getEventId(), nextRetryCount, result.getErrorMessage());
        } else {
            log.warn("[Integration][Delivery] failed deliveryId={} eventId={} retryCount={} nextRetryAt={} error={}",
                    entity.getId(), entity.getEventId(), nextRetryCount, nextRetryAt, result.getErrorMessage());
        }
    }

    public void markDead(final IntegrationDeliveryEntity entity, final String reason) {
        LambdaUpdateWrapper<IntegrationDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IntegrationDeliveryEntity::getId, entity.getId())
                .set(IntegrationDeliveryEntity::getStatus, IntegrationDeliveryStatus.DEAD)
                .set(IntegrationDeliveryEntity::getLastError, truncate(reason))
                .set(IntegrationDeliveryEntity::getNextRetryAt, null);
        mapper.update(null, wrapper);
        entity.setStatus(IntegrationDeliveryStatus.DEAD);
        entity.setLastError(reason);
        log.error("[Integration][Delivery] mark dead deliveryId={} eventId={} reason={}",
                entity.getId(), entity.getEventId(), reason);
    }

    public IntegrationDeliveryEntity findById(final Long id, final Long tenantId) {
        String previousTenant = TenantContext.getTenantId();
        try {
            if (tenantId != null) {
                TenantContext.setTenantId(String.valueOf(tenantId));
            }
            return mapper.selectById(id);
        } finally {
            restoreTenant(previousTenant);
        }
    }

    public boolean resetToNew(final Long id) {
        LambdaUpdateWrapper<IntegrationDeliveryEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IntegrationDeliveryEntity::getId, id)
                .set(IntegrationDeliveryEntity::getStatus, IntegrationDeliveryStatus.NEW)
                .set(IntegrationDeliveryEntity::getRetryCount, 0)
                .set(IntegrationDeliveryEntity::getNextRetryAt, LocalDateTime.now());
        return mapper.update(null, wrapper) > 0;
    }

    private Duration computeBackoff(final int retryCount) {
        long base = properties.getBaseDelaySeconds();
        long max = properties.getMaxDelaySeconds();
        double delay = base * Math.pow(2, Math.max(0, retryCount - 1));
        long seconds = Math.min(max, (long) delay);
        return Duration.ofSeconds(seconds);
    }

    private String truncate(final String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1024 ? message.substring(0, 1024) : message;
    }

    private void restoreTenant(final String previousTenant) {
        if (previousTenant == null) {
            TenantContext.clear();
        } else {
            TenantContext.setTenantId(previousTenant);
        }
    }
}
