package com.bluecone.app.core.create.api;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.id.core.Ulid128;

import java.util.List;

/**
 * 携带领域事件的创建操作回调。
 *
 * <p>用于在幂等创建模板中统一收集需要在事务提交后发布的领域事件。</p>
 */
@FunctionalInterface
public interface CreateWorkWithEvents<T> {

    /**
     * 执行具体创建逻辑并返回结果与待发布事件。
     *
     * @param internalId 内部 ULID
     * @param publicId   对外 public_id
     * @return 创建结果与领域事件列表
     */
    CreateWorkWithEventsResult<T> execute(Ulid128 internalId, String publicId);

    /**
     * 简单结果工厂，便于只关心返回值时使用。
     */
    static <T> CreateWorkWithEventsResult<T> result(T value, List<DomainEvent> events) {
        return new CreateWorkWithEventsResult<>(value, events);
    }
}

