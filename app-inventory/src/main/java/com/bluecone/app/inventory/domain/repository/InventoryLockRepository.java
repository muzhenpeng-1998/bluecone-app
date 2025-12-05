package com.bluecone.app.inventory.domain.repository;

import com.bluecone.app.inventory.domain.model.InventoryLock;
import java.util.List;

public interface InventoryLockRepository {

    InventoryLock findById(Long id);

    /**
     * 通过 tenantId + requestId 查询幂等锁记录。
     */
    InventoryLock findByRequestId(Long tenantId, String requestId);

    /**
     * 查询指定订单下所有 LOCKED 状态的锁记录。
     */
    List<InventoryLock> findLockedByOrder(Long tenantId, Long storeId, Long orderId);

    void save(InventoryLock lock);

    void update(InventoryLock lock);
}
