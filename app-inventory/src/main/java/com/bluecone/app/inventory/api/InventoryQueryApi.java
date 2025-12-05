package com.bluecone.app.inventory.api;

import com.bluecone.app.inventory.api.dto.BatchStockQuery;
import com.bluecone.app.inventory.api.dto.StockQuery;
import com.bluecone.app.inventory.api.dto.StockView;
import java.util.Map;

public interface InventoryQueryApi {

    StockView getStock(StockQuery query);

    Map<Long, StockView> batchGetStock(BatchStockQuery query);
}
