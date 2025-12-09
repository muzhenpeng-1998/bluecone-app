package com.bluecone.app.infra.event.outbox;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Outbox 仓储接口，负责保存事件并支持消费/状态迁移操作。
 */
public interface OutboxEventRepository {

    void save(OutboxEventDO event);

    /**
     * 查询可处理的事件列表。
     *
     * @param limit 每次最多拉取多少条
     */
    List<OutboxEventDO> findReadyEvents(int limit);

    /**
     * 将事件标记为已发送。
     */
    void markSent(Long id);

    /**
     * 将事件标记为发送失败并更新下次可用时间。
     */
    void markFailed(Long id, int nextRetryCount, LocalDateTime nextAvailableAt);
}

