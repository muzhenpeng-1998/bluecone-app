package com.bluecone.app.inventory.domain.repository;

import com.bluecone.app.inventory.domain.model.InventoryItem;
import com.bluecone.app.inventory.domain.type.InventoryItemType;

public interface InventoryItemRepository {

    InventoryItem findById(Long id);

    InventoryItem findByRef(Long tenantId, InventoryItemType itemType, Long refId);

    void save(InventoryItem item);

    void update(InventoryItem item);
}
