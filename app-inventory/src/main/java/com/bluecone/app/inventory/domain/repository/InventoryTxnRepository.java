package com.bluecone.app.inventory.domain.repository;

import com.bluecone.app.inventory.domain.model.InventoryTxn;

public interface InventoryTxnRepository {

    void save(InventoryTxn txn);

    InventoryTxn findByRequestId(Long tenantId, String requestId);
}
