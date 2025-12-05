package com.bluecone.app.inventory.infra.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.inventory.domain.model.InventoryTxn;
import com.bluecone.app.inventory.domain.repository.InventoryTxnRepository;
import com.bluecone.app.inventory.domain.type.InventoryTxnDirection;
import com.bluecone.app.inventory.domain.type.InventoryTxnType;
import com.bluecone.app.inventory.infra.mapper.InvTxnMapper;
import com.bluecone.app.inventory.infra.po.InvTxnDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryTxnRepositoryImpl implements InventoryTxnRepository {

    private final InvTxnMapper invTxnMapper;

    @Override
    public void save(InventoryTxn txn) {
        if (txn == null) {
            return;
        }
        InvTxnDO data = toDO(txn);
        invTxnMapper.insert(data);
        txn.setId(data.getId());
    }

    @Override
    public InventoryTxn findByRequestId(Long tenantId, String requestId) {
        InvTxnDO data = invTxnMapper.selectOne(new LambdaQueryWrapper<InvTxnDO>()
                .eq(InvTxnDO::getTenantId, tenantId)
                .eq(InvTxnDO::getRequestId, requestId));
        return toDomain(data);
    }

    private InventoryTxn toDomain(InvTxnDO data) {
        if (data == null) {
            return null;
        }
        return InventoryTxn.builder()
                .id(data.getId())
                .tenantId(data.getTenantId())
                .storeId(data.getStoreId())
                .itemId(data.getItemId())
                .locationId(data.getLocationId())
                .txnType(InventoryTxnType.fromCode(data.getTxnType()))
                .txnDirection(InventoryTxnDirection.fromCode(data.getTxnDirection()))
                .qty(data.getQty())
                .beforeTotal(data.getBeforeTotal())
                .afterTotal(data.getAfterTotal())
                .beforeLocked(data.getBeforeLocked())
                .afterLocked(data.getAfterLocked())
                .bizRefType(data.getBizRefType())
                .bizRefId(data.getBizRefId())
                .lockId(data.getLockId())
                .requestId(data.getRequestId())
                .extra(data.getExtra())
                .createdAt(data.getCreatedAt())
                .build();
    }

    private InvTxnDO toDO(InventoryTxn txn) {
        if (txn == null) {
            return null;
        }
        InvTxnDO data = new InvTxnDO();
        data.setId(txn.getId());
        data.setTenantId(txn.getTenantId());
        data.setStoreId(txn.getStoreId());
        data.setItemId(txn.getItemId());
        data.setLocationId(txn.getLocationId());
        data.setTxnType(txn.getTxnType() != null ? txn.getTxnType().getCode() : null);
        data.setTxnDirection(txn.getTxnDirection() != null ? txn.getTxnDirection().getCode() : null);
        data.setQty(txn.getQty());
        data.setBeforeTotal(txn.getBeforeTotal());
        data.setAfterTotal(txn.getAfterTotal());
        data.setBeforeLocked(txn.getBeforeLocked());
        data.setAfterLocked(txn.getAfterLocked());
        data.setBizRefType(txn.getBizRefType());
        data.setBizRefId(txn.getBizRefId());
        data.setLockId(txn.getLockId());
        data.setRequestId(txn.getRequestId());
        data.setExtra(txn.getExtra());
        data.setCreatedAt(txn.getCreatedAt());
        return data;
    }
}
