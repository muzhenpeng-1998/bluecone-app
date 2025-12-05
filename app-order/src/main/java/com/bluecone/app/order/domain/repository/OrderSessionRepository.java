package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.OrderSession;

public interface OrderSessionRepository {

    /**
     * 根据租户 + sessionId 查询会话。
     */
    OrderSession findBySessionId(Long tenantId, String sessionId);

    /**
     * 新建会话。
     */
    void save(OrderSession session);

    /**
     * 更新会话（包含 version 乐观锁控制）。
     */
    boolean updateWithVersionCheck(OrderSession session);
}
