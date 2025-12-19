package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

/**
 * Outbox 仓储实现：支持事件落库、采集和状态迁移。
 * 
 * Note: 此为旧版本实现，仅在 dev/test 环境使用。
 */
@Repository
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventMapper outboxEventMapper;

    @Override
    public void save(OutboxEventDO event) {
        OutboxEventPO po = convertToPO(event);
        outboxEventMapper.insert(po);
        event.setId(po.getId());
    }

    @Override
    public List<OutboxEventDO> findReadyEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<OutboxEventPO> poList = outboxEventMapper.selectList(
                new LambdaQueryWrapper<OutboxEventPO>()
                        .in(OutboxEventPO::getStatus, "NEW", "FAILED")
                        .le(OutboxEventPO::getNextRetryAt, now)
                        .orderByAsc(OutboxEventPO::getCreatedAt)
                        .last("LIMIT " + limit)
        );
        return poList.stream().map(this::convertToDO).collect(Collectors.toList());
    }

    @Override
    public void markSent(Long id) {
        OutboxEventPO update = new OutboxEventPO();
        update.setId(id);
        update.setStatus("SENT");
        update.setSentAt(LocalDateTime.now());
        update.setUpdatedAt(LocalDateTime.now());
        outboxEventMapper.updateById(update);
    }

    @Override
    public void markFailed(Long id, int nextRetryCount, LocalDateTime nextAvailableAt) {
        OutboxEventPO update = new OutboxEventPO();
        update.setId(id);
        update.setStatus("FAILED");
        update.setRetryCount(nextRetryCount);
        update.setNextRetryAt(nextAvailableAt);
        update.setUpdatedAt(LocalDateTime.now());
        outboxEventMapper.updateById(update);
    }

    /**
     * Convert DO to PO
     */
    private OutboxEventPO convertToPO(OutboxEventDO event) {
        OutboxEventPO po = new OutboxEventPO();
        po.setId(event.getId());
        po.setTenantId(event.getTenantId());
        po.setAggregateType(event.getAggregateType());
        po.setAggregateId(event.getAggregateId());
        po.setEventType(event.getEventType());
        po.setEventPayload(event.getEventBody());
        
        // Map status: 0->NEW, 1->SENT, 2->FAILED
        if (event.getStatus() != null) {
            switch (event.getStatus()) {
                case 0: po.setStatus("NEW"); break;
                case 1: po.setStatus("SENT"); break;
                case 2: po.setStatus("FAILED"); break;
                default: po.setStatus("NEW");
            }
        } else {
            po.setStatus("NEW");
        }
        
        po.setRetryCount(event.getRetryCount());
        po.setNextRetryAt(event.getAvailableAt());
        po.setCreatedAt(event.getCreatedAt());
        po.setUpdatedAt(event.getUpdatedAt());
        
        return po;
    }

    /**
     * Convert PO to DO
     */
    private OutboxEventDO convertToDO(OutboxEventPO po) {
        OutboxEventDO event = new OutboxEventDO();
        event.setId(po.getId());
        event.setTenantId(po.getTenantId());
        event.setAggregateType(po.getAggregateType());
        event.setAggregateId(po.getAggregateId());
        event.setEventType(po.getEventType());
        event.setEventBody(po.getEventPayload());
        
        // Map status: NEW->0, SENT->1, FAILED->2
        if (po.getStatus() != null) {
            switch (po.getStatus()) {
                case "NEW": event.setStatus(0); break;
                case "SENT": event.setStatus(1); break;
                case "FAILED": event.setStatus(2); break;
                default: event.setStatus(0);
            }
        } else {
            event.setStatus(0);
        }
        
        event.setRetryCount(po.getRetryCount());
        event.setAvailableAt(po.getNextRetryAt());
        event.setCreatedAt(po.getCreatedAt());
        event.setUpdatedAt(po.getUpdatedAt());
        
        return event;
    }
}

