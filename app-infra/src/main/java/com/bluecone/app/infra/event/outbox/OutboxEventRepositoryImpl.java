package com.bluecone.app.infra.event.outbox;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
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
        outboxEventMapper.insert(event);
    }

    @Override
    public List<OutboxEventDO> findReadyEvents(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return outboxEventMapper.selectList(
                new LambdaQueryWrapper<OutboxEventDO>()
                        .eq(OutboxEventDO::getStatus, 0)
                        .le(OutboxEventDO::getAvailableAt, now)
                        .orderByAsc(OutboxEventDO::getCreatedAt)
                        .last("LIMIT " + limit)
        );
    }

    @Override
    public void markSent(Long id) {
        OutboxEventDO update = new OutboxEventDO();
        update.setId(id);
        update.setStatus(1);
        update.setUpdatedAt(LocalDateTime.now());
        outboxEventMapper.updateById(update);
    }

    @Override
    public void markFailed(Long id, int nextRetryCount, LocalDateTime nextAvailableAt) {
        OutboxEventDO update = new OutboxEventDO();
        update.setId(id);
        update.setStatus(2);
        update.setRetryCount(nextRetryCount);
        update.setAvailableAt(nextAvailableAt);
        update.setUpdatedAt(LocalDateTime.now());
        outboxEventMapper.updateById(update);
    }
}

