package com.bluecone.app.inventory.api;

import com.bluecone.app.inventory.api.dto.AdjustStockCommand;
import com.bluecone.app.inventory.api.dto.ConfirmStockCommand;
import com.bluecone.app.inventory.api.dto.LockStockCommand;
import com.bluecone.app.inventory.api.dto.LockStockResult;
import com.bluecone.app.inventory.api.dto.ReleaseStockCommand;

public interface InventoryCommandApi {

    LockStockResult lockStock(LockStockCommand command);

    void confirmStock(ConfirmStockCommand command);

    void releaseStock(ReleaseStockCommand command);

    void adjustStock(AdjustStockCommand command);
}
