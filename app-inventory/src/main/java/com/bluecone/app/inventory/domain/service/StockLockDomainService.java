package com.bluecone.app.inventory.domain.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.inventory.domain.model.InventoryLock;
import com.bluecone.app.inventory.domain.model.InventoryPolicy;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.model.InventoryTxn;
import com.bluecone.app.inventory.domain.repository.InventoryLockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryTxnRepository;
import com.bluecone.app.inventory.domain.type.InventoryLockStatus;
import com.bluecone.app.inventory.domain.type.InventoryTxnDirection;
import com.bluecone.app.inventory.domain.type.InventoryTxnType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StockLockDomainService {

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final InventoryTxnRepository inventoryTxnRepository;

    /**
     * 锁定库存的领域操作。
     */
    public InventoryLock lock(InventoryStock stock,
                              InventoryPolicy policy,
                              Long orderId,
                              Long orderItemId,
                              long lockQty,
                              String requestId,
                              LocalDateTime expireAt) {
        if (stock == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "库存不存在，无法锁定");
        }
        if (lockQty <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "锁定数量必须大于0");
        }
        if (!StringUtils.hasText(requestId)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "请求ID不能为空");
        }

        InventoryLock existing = inventoryLockRepository.findByRequestId(stock.getTenantId(), requestId);
        if (existing != null) {
            // TODO: 可校验订单号/数量一致性
            return existing;
        }

        if (policy != null && !policy.isEnabled()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "库存策略未启用");
        }

        if (!stock.canLock(lockQty, policy)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "可用库存不足，无法锁定");
        }

        long beforeTotal = nullSafe(stock.getTotalQty());
        long beforeLocked = nullSafe(stock.getLockedQty());

        boolean updated = inventoryStockRepository.tryIncreaseLocked(stock, lockQty);
        if (!updated) {
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, String.format(
                    "锁定库存失败，可能并发冲突或库存不足，tenantId=%s,storeId=%s,itemId=%s,lockQty=%s",
                    stock.getTenantId(), stock.getStoreId(), stock.getItemId(), lockQty));
        }

        stock.applyLock(lockQty);
        if (stock.getVersion() != null) {
            stock.setVersion(stock.getVersion() + 1);
        }

        InventoryLock lock = InventoryLock.createNew(
                stock.getTenantId(),
                stock.getStoreId(),
                stock.getItemId(),
                stock.getLocationId(),
                orderId,
                orderItemId,
                lockQty,
                requestId,
                expireAt);
        inventoryLockRepository.save(lock);

        InventoryTxn txn = InventoryTxn.forLock(
                stock.getTenantId(),
                stock.getStoreId(),
                stock.getItemId(),
                stock.getLocationId(),
                lockQty,
                beforeTotal,
                stock.getTotalQty(),
                beforeLocked,
                stock.getLockedQty(),
                lock.getId(),
                requestId,
                "ORDER",
                orderId);
        txn.setTxnDirection(InventoryTxnDirection.OUT);
        txn.setTxnType(InventoryTxnType.LOCK);
        txn.setCreatedAt(LocalDateTime.now());
        inventoryTxnRepository.save(txn);

        return lock;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
