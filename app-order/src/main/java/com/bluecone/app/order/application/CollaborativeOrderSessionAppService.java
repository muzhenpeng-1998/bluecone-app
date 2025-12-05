package com.bluecone.app.order.application;

import com.bluecone.app.order.domain.model.OrderSession;

public interface CollaborativeOrderSessionAppService {

    /**
     * 创建一个新的一起点单会话。
     */
    OrderSession createSession(Long tenantId, Long storeId, Long tableId, Long hostUserId);

    /**
     * 根据会话ID查询会话。
     */
    OrderSession getSession(Long tenantId, String sessionId);

    /**
     * 更新会话的购物车快照，并做 version 乐观锁控制。
     */
    OrderSession updateSnapshotWithVersionCheck(Long tenantId, String sessionId, Integer expectedVersion, String snapshotJson);

    /**
     * 确认后关闭会话（标记为 CONFIRMED）。
     */
    void closeSessionAfterConfirm(Long tenantId, String sessionId);
}
