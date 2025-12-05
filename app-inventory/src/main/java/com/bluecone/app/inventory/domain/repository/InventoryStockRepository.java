package com.bluecone.app.inventory.domain.repository;

import com.bluecone.app.inventory.domain.model.InventoryStock;

public interface InventoryStockRepository {

    InventoryStock findByTenantStoreItem(Long tenantId,
                                         Long storeId,
                                         Long itemId,
                                         Long locationId);

    void save(InventoryStock stock);

    void update(InventoryStock stock);

    /**
     * 尝试基于乐观锁增加锁定库存。
     *
     * @return true 表示更新成功，false 表示版本冲突或库存不足导致更新失败。
     */
    boolean tryIncreaseLocked(InventoryStock stock, long lockQty);

    /**
     * 尝试基于乐观锁扣减库存（支付成功或确认后）。
     */
    boolean tryDeduct(InventoryStock stock, long deductQty);
}
