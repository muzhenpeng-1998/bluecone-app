package com.bluecone.app.infra.event.consume;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only queries on event consume records for ops console drill-down.
 */
@Repository
public class EventConsumeRecordOpsRepository {

    private final EventConsumeRecordMapper mapper;

    public EventConsumeRecordOpsRepository(final EventConsumeRecordMapper mapper) {
        this.mapper = mapper;
    }

    public List<EventConsumeRecordDO> listConsume(String consumerGroup,
                                                  Integer status,
                                                  boolean retryOnly,
                                                  LocalDateTime now,
                                                  Long beforeId,
                                                  int limit) {
        LambdaQueryWrapper<EventConsumeRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(
                        EventConsumeRecordDO::getId,
                        EventConsumeRecordDO::getTenantId,
                        EventConsumeRecordDO::getConsumerGroup,
                        EventConsumeRecordDO::getEventId,
                        EventConsumeRecordDO::getEventType,
                        EventConsumeRecordDO::getStatus,
                        EventConsumeRecordDO::getLockedBy,
                        EventConsumeRecordDO::getLockedUntil,
                        EventConsumeRecordDO::getNextRetryAt,
                        EventConsumeRecordDO::getRetryCount,
                        EventConsumeRecordDO::getErrorMsg,
                        EventConsumeRecordDO::getCreatedAt,
                        EventConsumeRecordDO::getUpdatedAt
                )
                .eq(consumerGroup != null, EventConsumeRecordDO::getConsumerGroup, consumerGroup)
                .eq(status != null, EventConsumeRecordDO::getStatus, status);

        if (retryOnly) {
            wrapper.le(EventConsumeRecordDO::getNextRetryAt, now);
        }
        if (beforeId != null) {
            wrapper.lt(EventConsumeRecordDO::getId, beforeId);
        }
        wrapper.orderByDesc(EventConsumeRecordDO::getId)
                .last("limit " + limit);
        return mapper.selectList(wrapper);
    }
}

