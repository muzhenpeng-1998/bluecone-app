package com.bluecone.app.inventory.domain.repository;

import com.bluecone.app.inventory.domain.model.InventoryPolicy;

public interface InventoryPolicyRepository {

    InventoryPolicy findByItem(Long tenantId, Long storeId, Long itemId);

    void save(InventoryPolicy policy);

    void update(InventoryPolicy policy);
}
