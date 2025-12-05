package com.bluecone.app.inventory.infra.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.inventory.domain.model.InventoryLock;
import com.bluecone.app.inventory.domain.repository.InventoryLockRepository;
import com.bluecone.app.inventory.domain.type.InventoryLockStatus;
import com.bluecone.app.inventory.infra.mapper.InvStockLockMapper;
import com.bluecone.app.inventory.infra.po.InvStockLockDO;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryLockRepositoryImpl implements InventoryLockRepository {

    private final InvStockLockMapper invStockLockMapper;

    @Override
    public InventoryLock findById(Long id) {
        InvStockLockDO data = invStockLockMapper.selectById(id);
        return toDomain(data);
    }

    @Override
    public InventoryLock findByRequestId(Long tenantId, String requestId) {
        InvStockLockDO data = invStockLockMapper.selectOne(new LambdaQueryWrapper<InvStockLockDO>()
                .eq(InvStockLockDO::getTenantId, tenantId)
                .eq(InvStockLockDO::getRequestId, requestId));
        return toDomain(data);
    }

    @Override
    public List<InventoryLock> findLockedByOrder(Long tenantId, Long storeId, Long orderId) {
        List<InvStockLockDO> list = invStockLockMapper.selectList(new LambdaQueryWrapper<InvStockLockDO>()
                .eq(InvStockLockDO::getTenantId, tenantId)
                .eq(InvStockLockDO::getStoreId, storeId)
                .eq(InvStockLockDO::getOrderId, orderId)
                .eq(InvStockLockDO::getStatus, InventoryLockStatus.LOCKED.getCode()));
        return list.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void save(InventoryLock lock) {
        if (lock == null) {
            return;
        }
        InvStockLockDO data = toDO(lock);
        invStockLockMapper.insert(data);
        lock.setId(data.getId());
    }

    @Override
    public void update(InventoryLock lock) {
        if (lock == null || lock.getId() == null) {
            return;
        }
        invStockLockMapper.updateById(toDO(lock));
    }

    private InventoryLock toDomain(InvStockLockDO data) {
        if (data == null) {
            return null;
        }
        return InventoryLock.builder()
                .id(data.getId())
                .tenantId(data.getTenantId())
                .storeId(data.getStoreId())
                .itemId(data.getItemId())
                .locationId(data.getLocationId())
                .orderId(data.getOrderId())
                .orderItemId(data.getOrderItemId())
                .lockQty(data.getLockQty())
                .status(InventoryLockStatus.fromCode(data.getStatus()))
                .expireAt(data.getExpireAt())
                .requestId(data.getRequestId())
                .remark(data.getRemark())
                .ext(data.getExt())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }

    private InvStockLockDO toDO(InventoryLock lock) {
        if (lock == null) {
            return null;
        }
        InvStockLockDO data = new InvStockLockDO();
        data.setId(lock.getId());
        data.setTenantId(lock.getTenantId());
        data.setStoreId(lock.getStoreId());
        data.setItemId(lock.getItemId());
        data.setLocationId(lock.getLocationId());
        data.setOrderId(lock.getOrderId());
        data.setOrderItemId(lock.getOrderItemId());
        data.setLockQty(lock.getLockQty());
        data.setStatus(lock.getStatus() != null ? lock.getStatus().getCode() : null);
        data.setExpireAt(lock.getExpireAt());
        data.setRequestId(lock.getRequestId());
        data.setRemark(lock.getRemark());
        data.setExt(lock.getExt());
        data.setCreatedAt(lock.getCreatedAt());
        data.setUpdatedAt(lock.getUpdatedAt());
        return data;
    }
}
