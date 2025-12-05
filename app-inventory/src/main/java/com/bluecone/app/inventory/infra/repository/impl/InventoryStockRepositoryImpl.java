package com.bluecone.app.inventory.infra.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import com.bluecone.app.inventory.infra.mapper.InvStockMapper;
import com.bluecone.app.inventory.infra.po.InvStockDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryStockRepositoryImpl implements InventoryStockRepository {

    private final InvStockMapper invStockMapper;

    @Override
    public InventoryStock findByTenantStoreItem(Long tenantId, Long storeId, Long itemId, Long locationId) {
        InvStockDO data = invStockMapper.selectOne(new LambdaQueryWrapper<InvStockDO>()
                .eq(InvStockDO::getTenantId, tenantId)
                .eq(InvStockDO::getStoreId, storeId)
                .eq(InvStockDO::getItemId, itemId)
                .eq(InvStockDO::getLocationId, locationId));
        return toDomain(data);
    }

    @Override
    public void save(InventoryStock stock) {
        if (stock == null) {
            return;
        }
        InvStockDO data = toDO(stock);
        invStockMapper.insert(data);
        stock.setId(data.getId());
    }

    @Override
    public void update(InventoryStock stock) {
        if (stock == null || stock.getId() == null) {
            return;
        }
        invStockMapper.updateById(toDO(stock));
    }

    @Override
    public boolean tryIncreaseLocked(InventoryStock stock, long lockQty) {
        if (stock == null) {
            return false;
        }
        int affected = invStockMapper.tryIncreaseLockedQty(
                stock.getTenantId(),
                stock.getStoreId(),
                stock.getItemId(),
                stock.getLocationId(),
                lockQty,
                stock.getVersion());
        return affected > 0;
    }

    @Override
    public boolean tryDeduct(InventoryStock stock, long deductQty) {
        if (stock == null) {
            return false;
        }
        int affected = invStockMapper.tryDeductStock(
                stock.getTenantId(),
                stock.getStoreId(),
                stock.getItemId(),
                stock.getLocationId(),
                deductQty,
                stock.getVersion());
        return affected > 0;
    }

    private InventoryStock toDomain(InvStockDO data) {
        if (data == null) {
            return null;
        }
        return InventoryStock.builder()
                .id(data.getId())
                .tenantId(data.getTenantId())
                .storeId(data.getStoreId())
                .itemId(data.getItemId())
                .locationId(data.getLocationId())
                .totalQty(data.getTotalQty())
                .lockedQty(data.getLockedQty())
                .availableQty(data.getAvailableQty())
                .safetyStock(data.getSafetyStock())
                .version(data.getVersion())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }

    private InvStockDO toDO(InventoryStock stock) {
        if (stock == null) {
            return null;
        }
        InvStockDO data = new InvStockDO();
        data.setId(stock.getId());
        data.setTenantId(stock.getTenantId());
        data.setStoreId(stock.getStoreId());
        data.setItemId(stock.getItemId());
        data.setLocationId(stock.getLocationId());
        data.setTotalQty(stock.getTotalQty());
        data.setLockedQty(stock.getLockedQty());
        data.setAvailableQty(stock.getAvailableQty());
        data.setSafetyStock(stock.getSafetyStock());
        data.setVersion(stock.getVersion());
        data.setCreatedAt(stock.getCreatedAt());
        data.setUpdatedAt(stock.getUpdatedAt());
        return data;
    }
}
