package com.bluecone.app.inventory.domain.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.inventory.domain.model.InventoryLock;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.model.InventoryTxn;
import com.bluecone.app.inventory.domain.repository.InventoryLockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryTxnRepository;
import com.bluecone.app.inventory.domain.type.InventoryLockStatus;
import com.bluecone.app.inventory.domain.type.InventoryTxnType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class StockDeductDomainService {

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final InventoryTxnRepository inventoryTxnRepository;

    /**
     * 确认一批锁记录并扣减库存（通常在支付成功后调用）。
     */
    public void confirmLocksAndDeduct(List<InventoryLock> locks, String requestId) {
        if (CollectionUtils.isEmpty(locks)) {
            return;
        }
        for (InventoryLock lock : locks) {
            if (lock == null || lock.getStatus() != InventoryLockStatus.LOCKED) {
                continue;
            }
            InventoryStock stock = inventoryStockRepository.findByTenantStoreItem(
                    lock.getTenantId(),
                    lock.getStoreId(),
                    lock.getItemId(),
                    lock.getLocationId());
            if (stock == null) {
                throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, String.format(
                        "库存不存在，无法扣减，tenantId=%s,storeId=%s,itemId=%s,lockId=%s",
                        lock.getTenantId(), lock.getStoreId(), lock.getItemId(), lock.getId()));
            }

            long lockQty = lock.getLockQty() == null ? 0L : lock.getLockQty();
            long beforeTotal = nullSafe(stock.getTotalQty());
            long beforeLocked = nullSafe(stock.getLockedQty());

            boolean updated = inventoryStockRepository.tryDeduct(stock, lockQty);
            if (!updated) {
                throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, String.format(
                        "扣减库存失败，可能并发冲突或库存不足，tenantId=%s,storeId=%s,itemId=%s,lockId=%s,qty=%s",
                        stock.getTenantId(), stock.getStoreId(), stock.getItemId(), lock.getId(), lockQty));
            }

            stock.applyDeduct(lockQty);
            if (stock.getVersion() != null) {
                stock.setVersion(stock.getVersion() + 1);
            }

            long afterTotal = nullSafe(stock.getTotalQty());
            long afterLocked = nullSafe(stock.getLockedQty());

            lock.markConfirmed();
            inventoryLockRepository.update(lock);

            InventoryTxn txn = InventoryTxn.forDeduct(
                    stock.getTenantId(),
                    stock.getStoreId(),
                    stock.getItemId(),
                    stock.getLocationId(),
                    lockQty,
                    beforeTotal,
                    afterTotal,
                    beforeLocked,
                    afterLocked,
                    lock.getId(),
                    requestId,
                    "ORDER",
                    lock.getOrderId());
            txn.setCreatedAt(LocalDateTime.now());
            txn.setTxnType(InventoryTxnType.DEDUCT);
            inventoryTxnRepository.save(txn);
        }
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
