package com.bluecone.app.inventory.domain.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.inventory.domain.model.InventoryLock;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.model.InventoryTxn;
import com.bluecone.app.inventory.domain.repository.InventoryLockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryTxnRepository;
import com.bluecone.app.inventory.domain.type.InventoryLockStatus;
import com.bluecone.app.inventory.domain.type.InventoryTxnDirection;
import com.bluecone.app.inventory.domain.type.InventoryTxnType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class StockReleaseDomainService {

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final InventoryTxnRepository inventoryTxnRepository;

    /**
     * 释放一批锁记录（订单取消或超时），将锁定库存还原为可用库存。
     */
    public void releaseLocks(List<InventoryLock> locks, String requestId, boolean expired) {
        if (CollectionUtils.isEmpty(locks)) {
            return;
        }
        List<InventoryLock> pending = locks.stream()
                .filter(Objects::nonNull)
                .filter(lock -> lock.getStatus() == InventoryLockStatus.LOCKED)
                .collect(Collectors.toList());
        if (pending.isEmpty()) {
            return;
        }

        for (InventoryLock lock : pending) {
            InventoryStock stock = inventoryStockRepository.findByTenantStoreItem(
                    lock.getTenantId(),
                    lock.getStoreId(),
                    lock.getItemId(),
                    lock.getLocationId());
            if (stock == null) {
                throw new BizException(CommonErrorCode.SYSTEM_ERROR, String.format(
                        "库存不存在，无法释放，tenantId=%s,storeId=%s,itemId=%s,lockId=%s",
                        lock.getTenantId(), lock.getStoreId(), lock.getItemId(), lock.getId()));
            }

            long releaseQty = lock.getLockQty() == null ? 0L : lock.getLockQty();
            long beforeTotal = nullSafe(stock.getTotalQty());
            long beforeLocked = nullSafe(stock.getLockedQty());

            stock.applyRelease(releaseQty);
            if (stock.getVersion() != null) {
                stock.setVersion(stock.getVersion() + 1);
            }
            inventoryStockRepository.update(stock);

            long afterTotal = nullSafe(stock.getTotalQty());
            long afterLocked = nullSafe(stock.getLockedQty());

            if (expired) {
                lock.markExpired();
            } else {
                lock.markReleased();
            }
            inventoryLockRepository.update(lock);

            InventoryTxn txn = InventoryTxn.forUnlock(
                    stock.getTenantId(),
                    stock.getStoreId(),
                    stock.getItemId(),
                    stock.getLocationId(),
                    releaseQty,
                    beforeTotal,
                    afterTotal,
                    beforeLocked,
                    afterLocked,
                    lock.getId(),
                    requestId,
                    "ORDER",
                    lock.getOrderId());
            txn.setTxnDirection(InventoryTxnDirection.IN);
            txn.setTxnType(InventoryTxnType.UNLOCK);
            txn.setCreatedAt(LocalDateTime.now());
            inventoryTxnRepository.save(txn);
        }
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
