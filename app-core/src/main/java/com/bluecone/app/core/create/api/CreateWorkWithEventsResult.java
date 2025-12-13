package com.bluecone.app.core.create.api;

import com.bluecone.app.core.event.DomainEvent;

import java.util.List;

/**
 * 携带领域事件的创建结果。
 *
 * @param <T> 业务返回类型
 */
public record CreateWorkWithEventsResult<T>(
        T value,
        List<DomainEvent> events
) {
}

