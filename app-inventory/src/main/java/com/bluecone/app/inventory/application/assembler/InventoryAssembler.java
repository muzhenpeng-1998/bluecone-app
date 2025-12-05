package com.bluecone.app.inventory.application.assembler;

import com.bluecone.app.inventory.api.dto.StockView;
import com.bluecone.app.inventory.domain.model.InventoryStock;

public final class InventoryAssembler {

    private InventoryAssembler() {
    }

    public static StockView toStockView(InventoryStock stock) {
        if (stock == null) {
            return null;
        }
        StockView view = new StockView();
        view.setTenantId(stock.getTenantId());
        view.setStoreId(stock.getStoreId());
        view.setItemId(stock.getItemId());
        view.setLocationId(stock.getLocationId());
        view.setTotalQty(stock.getTotalQty());
        view.setLockedQty(stock.getLockedQty());
        view.setAvailableQty(stock.getAvailableQty());
        view.setSafetyStock(stock.getSafetyStock());
        return view;
    }
}
