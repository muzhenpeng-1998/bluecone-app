package com.bluecone.app.inventory.domain.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.model.InventoryTxn;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryTxnRepository;
import com.bluecone.app.inventory.domain.type.InventoryTxnDirection;
import com.bluecone.app.inventory.domain.type.InventoryTxnType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockAdjustDomainService {

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryTxnRepository inventoryTxnRepository;

    /**
     * 入库（采购、盘盈等），增加 totalQty 和 availableQty。
     */
    public void inbound(InventoryStock stock,
                        long qty,
                        String bizRefType,
                        Long bizRefId,
                        String requestId) {
        if (stock == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "库存不存在，无法入库");
        }
        if (qty <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "入库数量必须大于0");
        }
        long beforeTotal = nullSafe(stock.getTotalQty());
        long beforeLocked = nullSafe(stock.getLockedQty());
        stock.setTotalQty(beforeTotal + qty);
        stock.setAvailableQty(nullSafe(stock.getAvailableQty()) + qty);
        if (stock.getVersion() != null) {
            stock.setVersion(stock.getVersion() + 1);
        }
        inventoryStockRepository.update(stock);

        InventoryTxn txn = InventoryTxn.builder()
                .tenantId(stock.getTenantId())
                .storeId(stock.getStoreId())
                .itemId(stock.getItemId())
                .locationId(stock.getLocationId())
                .txnType(InventoryTxnType.INBOUND)
                .txnDirection(InventoryTxnDirection.IN)
                .qty(qty)
                .beforeTotal(beforeTotal)
                .afterTotal(stock.getTotalQty())
                .beforeLocked(beforeLocked)
                .afterLocked(stock.getLockedQty())
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .requestId(requestId)
                .createdAt(LocalDateTime.now())
                .build();
        inventoryTxnRepository.save(txn);
    }

    /**
     * 出库（报废、赠送等），减少 totalQty 和 availableQty。
     */
    public void outbound(InventoryStock stock,
                         long qty,
                         String bizRefType,
                         Long bizRefId,
                         String requestId) {
        if (stock == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "库存不存在，无法出库");
        }
        if (qty <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "出库数量必须大于0");
        }
        long beforeTotal = nullSafe(stock.getTotalQty());
        long beforeLocked = nullSafe(stock.getLockedQty());
        long beforeAvailable = nullSafe(stock.getAvailableQty());
        if (beforeAvailable < qty || beforeTotal < qty) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "库存不足，无法出库");
        }

        stock.setTotalQty(beforeTotal - qty);
        stock.setAvailableQty(beforeAvailable - qty);
        if (stock.getVersion() != null) {
            stock.setVersion(stock.getVersion() + 1);
        }
        inventoryStockRepository.update(stock);

        InventoryTxn txn = InventoryTxn.builder()
                .tenantId(stock.getTenantId())
                .storeId(stock.getStoreId())
                .itemId(stock.getItemId())
                .locationId(stock.getLocationId())
                .txnType(InventoryTxnType.OUTBOUND)
                .txnDirection(InventoryTxnDirection.OUT)
                .qty(qty)
                .beforeTotal(beforeTotal)
                .afterTotal(stock.getTotalQty())
                .beforeLocked(beforeLocked)
                .afterLocked(stock.getLockedQty())
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .requestId(requestId)
                .createdAt(LocalDateTime.now())
                .build();
        inventoryTxnRepository.save(txn);
    }

    /**
     * 盘点调整，将库存调整为目标数量。
     *
     * @param targetTotalQty 盘点后的总数量（最小单位）
     */
    public void adjustTo(InventoryStock stock,
                         long targetTotalQty,
                         String bizRefType,
                         Long bizRefId,
                         String requestId) {
        if (stock == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "库存不存在，无法盘点调整");
        }
        long currentTotal = nullSafe(stock.getTotalQty());
        long diff = targetTotalQty - currentTotal;
        if (diff == 0) {
            return;
        }
        if (targetTotalQty < nullSafe(stock.getLockedQty())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "目标库存低于已锁定库存，无法调整");
        }

        long beforeTotal = currentTotal;
        long beforeLocked = nullSafe(stock.getLockedQty());
        if (diff > 0) {
            stock.setTotalQty(currentTotal + diff);
            stock.setAvailableQty(nullSafe(stock.getAvailableQty()) + diff);
        } else {
            long delta = Math.abs(diff);
            long available = nullSafe(stock.getAvailableQty());
            if (available < delta) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "可用库存不足，无法调整");
            }
            stock.setTotalQty(currentTotal - delta);
            stock.setAvailableQty(available - delta);
        }
        if (stock.getVersion() != null) {
            stock.setVersion(stock.getVersion() + 1);
        }
        inventoryStockRepository.update(stock);

        InventoryTxn txn = InventoryTxn.builder()
                .tenantId(stock.getTenantId())
                .storeId(stock.getStoreId())
                .itemId(stock.getItemId())
                .locationId(stock.getLocationId())
                .txnType(InventoryTxnType.ADJUST)
                .txnDirection(diff > 0 ? InventoryTxnDirection.IN : InventoryTxnDirection.OUT)
                .qty(Math.abs(diff))
                .beforeTotal(beforeTotal)
                .afterTotal(stock.getTotalQty())
                .beforeLocked(beforeLocked)
                .afterLocked(stock.getLockedQty())
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .requestId(requestId)
                .createdAt(LocalDateTime.now())
                .build();
        inventoryTxnRepository.save(txn);
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
