package com.bluecone.app.inventory.application.service;

import com.bluecone.app.infra.cache.annotation.Cached;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.inventory.api.InventoryQueryApi;
import com.bluecone.app.inventory.api.dto.BatchStockQuery;
import com.bluecone.app.inventory.api.dto.StockQuery;
import com.bluecone.app.inventory.api.dto.StockView;
import com.bluecone.app.inventory.application.assembler.InventoryAssembler;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryQueryApplicationService implements InventoryQueryApi {

    private final InventoryStockRepository inventoryStockRepository;

    @Override
    @Cached(profile = CacheProfileName.INVENTORY_STOCK,
            key = "#query.tenantId + ':' + #query.storeId + ':' + (#query.locationId == null ? 0 : #query.locationId) + ':' + #query.itemId")
    public StockView getStock(StockQuery query) {
        Objects.requireNonNull(query.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(query.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(query.getItemId(), "itemId 不能为空");

        Long tenantId = query.getTenantId();
        Long storeId = query.getStoreId();
        Long itemId = query.getItemId();
        Long locationId = query.getLocationId() != null ? query.getLocationId() : 0L;

        InventoryStock stock = inventoryStockRepository.findByTenantStoreItem(
                tenantId, storeId, itemId, locationId);

        if (stock == null) {
            StockView view = new StockView();
            view.setTenantId(tenantId);
            view.setStoreId(storeId);
            view.setItemId(itemId);
            view.setLocationId(locationId);
            view.setTotalQty(0L);
            view.setLockedQty(0L);
            view.setAvailableQty(0L);
            view.setSafetyStock(0L);
            return view;
        }

        return InventoryAssembler.toStockView(stock);
    }

    @Override
    public Map<Long, StockView> batchGetStock(BatchStockQuery query) {
        Objects.requireNonNull(query.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(query.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(query.getItemIds(), "itemIds 不能为空");

        Map<Long, StockView> result = new HashMap<>();
        List<Long> itemIds = query.getItemIds();
        if (itemIds == null || itemIds.isEmpty()) {
            return result;
        }
        for (Long itemId : itemIds) {
            if (itemId == null) {
                continue;
            }
            StockView view = getStock(StockQuery.builder()
                    .tenantId(query.getTenantId())
                    .storeId(query.getStoreId())
                    .itemId(itemId)
                    .locationId(query.getLocationId())
                    .build());
            if (view != null) {
                result.put(itemId, view);
            }
        }
        return result;
    }
}
