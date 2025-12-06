// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/repository/OutboxMessageRepository.java
package com.bluecone.app.infra.outbox.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.mapper.OutboxMessageMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Outbox 持久层封装，隐藏 MyBatis-Plus 细节。
 */
@Repository
public class OutboxMessageRepository {

    private final OutboxMessageMapper mapper;

    public OutboxMessageRepository(final OutboxMessageMapper mapper) {
        this.mapper = mapper;
    }

    public void save(final OutboxMessageEntity entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        mapper.insert(entity);
    }

    public List<OutboxMessageEntity> findDueMessages(final List<OutboxMessageStatus> statuses,
                                                     final LocalDateTime now,
                                                     final int limit) {
        LambdaQueryWrapper<OutboxMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OutboxMessageEntity::getStatus, statuses)
                // next_retry_at 为空视为可立即投递，或小于等于当前时间
                .and(w -> w.isNull(OutboxMessageEntity::getNextRetryAt)
                        .or()
                        .le(OutboxMessageEntity::getNextRetryAt, now))
                .orderByAsc(OutboxMessageEntity::getNextRetryAt)
                .last("limit " + limit);
        return mapper.selectList(wrapper);
    }

    public boolean markDone(final Long id) {
        LambdaUpdateWrapper<OutboxMessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxMessageEntity::getId, id)
                .set(OutboxMessageEntity::getStatus, OutboxMessageStatus.DONE)
                .set(OutboxMessageEntity::getNextRetryAt, null);
        return mapper.update(null, wrapper) > 0;
    }

    public boolean markFailed(final Long id,
                              final int retryCount,
                              final LocalDateTime nextRetryAt,
                              final boolean dead) {
        LambdaUpdateWrapper<OutboxMessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxMessageEntity::getId, id)
                .set(OutboxMessageEntity::getStatus, dead ? OutboxMessageStatus.DEAD : OutboxMessageStatus.FAILED)
                .set(OutboxMessageEntity::getRetryCount, retryCount)
                .set(OutboxMessageEntity::getNextRetryAt, nextRetryAt);
        return mapper.update(null, wrapper) > 0;
    }

    public int cleanOldMessages(final List<OutboxMessageStatus> statuses, final LocalDateTime beforeTime, final int batchSize) {
        Page<OutboxMessageEntity> page = new Page<>(1, batchSize);
        LambdaQueryWrapper<OutboxMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(OutboxMessageEntity::getStatus, statuses)
                .lt(OutboxMessageEntity::getUpdatedAt, beforeTime);
        List<OutboxMessageEntity> list = mapper.selectPage(page, wrapper).getRecords();
        int deleted = 0;
        for (OutboxMessageEntity entity : list) {
            deleted += mapper.deleteById(entity.getId());
        }
        return deleted;
    }

    public Page<OutboxMessageEntity> pageQuery(final int pageNum,
                                               final int pageSize,
                                               final OutboxMessageStatus status,
                                               final String eventType,
                                               final Long tenantId,
                                               final LocalDate from,
                                               final LocalDate to) {
        Page<OutboxMessageEntity> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OutboxMessageEntity> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(OutboxMessageEntity::getStatus, status);
        }
        if (eventType != null) {
            wrapper.eq(OutboxMessageEntity::getEventType, eventType);
        }
        if (tenantId != null) {
            wrapper.eq(OutboxMessageEntity::getTenantId, tenantId);
        }
        if (from != null) {
            wrapper.ge(OutboxMessageEntity::getCreatedAt, from.atStartOfDay());
        }
        if (to != null) {
            wrapper.le(OutboxMessageEntity::getCreatedAt, to.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(OutboxMessageEntity::getCreatedAt);
        return mapper.selectPage(page, wrapper);
    }

    public boolean markDead(final Long id) {
        LambdaUpdateWrapper<OutboxMessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxMessageEntity::getId, id)
                .set(OutboxMessageEntity::getStatus, OutboxMessageStatus.DEAD)
                .set(OutboxMessageEntity::getNextRetryAt, null);
        return mapper.update(null, wrapper) > 0;
    }

    public boolean resetToNew(final Long id, final boolean incrementRetry) {
        LambdaUpdateWrapper<OutboxMessageEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(OutboxMessageEntity::getId, id)
                .set(OutboxMessageEntity::getStatus, OutboxMessageStatus.NEW)
                .set(OutboxMessageEntity::getNextRetryAt, LocalDateTime.now());
        if (!incrementRetry) {
            wrapper.set(OutboxMessageEntity::getRetryCount, 0);
        } else {
            wrapper.setSql("retry_count = retry_count + 1");
        }
        return mapper.update(null, wrapper) > 0;
    }

    /**
     * 统计指定状态、事件类型前缀、且创建时间早于阈值的消息数量，用于健康巡检。
     */
    public long countByStatusAndPrefix(final OutboxMessageStatus status,
                                       final String eventTypePrefix,
                                       final LocalDateTime createdBefore) {
        LambdaQueryWrapper<OutboxMessageEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OutboxMessageEntity::getStatus, status);
        if (eventTypePrefix != null && !eventTypePrefix.isBlank()) {
            wrapper.likeRight(OutboxMessageEntity::getEventType, eventTypePrefix);
        }
        if (createdBefore != null) {
            wrapper.le(OutboxMessageEntity::getCreatedAt, createdBefore);
        }
        return mapper.selectCount(wrapper);
    }
}
