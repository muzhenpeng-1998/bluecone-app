package com.bluecone.app.inventory.domain.model;

import com.bluecone.app.inventory.domain.type.InventoryTxnDirection;
import com.bluecone.app.inventory.domain.type.InventoryTxnType;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTxn implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private InventoryTxnType txnType;

    private InventoryTxnDirection txnDirection;

    private Long qty;

    private Long beforeTotal;

    private Long afterTotal;

    private Long beforeLocked;

    private Long afterLocked;

    private String bizRefType;

    private Long bizRefId;

    private Long lockId;

    private String requestId;

    private String extra;

    private LocalDateTime createdAt;

    public static InventoryTxn forLock(Long tenantId,
                                       Long storeId,
                                       Long itemId,
                                       Long locationId,
                                       Long qty,
                                       Long beforeTotal,
                                       Long afterTotal,
                                       Long beforeLocked,
                                       Long afterLocked,
                                       Long lockId,
                                       String requestId,
                                       String bizRefType,
                                       Long bizRefId) {
        return InventoryTxn.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .itemId(itemId)
                .locationId(locationId)
                .qty(qty)
                .beforeTotal(beforeTotal)
                .afterTotal(afterTotal)
                .beforeLocked(beforeLocked)
                .afterLocked(afterLocked)
                .lockId(lockId)
                .requestId(requestId)
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .txnType(InventoryTxnType.LOCK)
                .txnDirection(InventoryTxnDirection.OUT)
                .build();
    }

    public static InventoryTxn forUnlock(Long tenantId,
                                         Long storeId,
                                         Long itemId,
                                         Long locationId,
                                         Long qty,
                                         Long beforeTotal,
                                         Long afterTotal,
                                         Long beforeLocked,
                                         Long afterLocked,
                                         Long lockId,
                                         String requestId,
                                         String bizRefType,
                                         Long bizRefId) {
        return InventoryTxn.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .itemId(itemId)
                .locationId(locationId)
                .qty(qty)
                .beforeTotal(beforeTotal)
                .afterTotal(afterTotal)
                .beforeLocked(beforeLocked)
                .afterLocked(afterLocked)
                .lockId(lockId)
                .requestId(requestId)
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .txnType(InventoryTxnType.UNLOCK)
                .txnDirection(InventoryTxnDirection.IN)
                .build();
    }

    public static InventoryTxn forDeduct(Long tenantId,
                                         Long storeId,
                                         Long itemId,
                                         Long locationId,
                                         Long qty,
                                         Long beforeTotal,
                                         Long afterTotal,
                                         Long beforeLocked,
                                         Long afterLocked,
                                         Long lockId,
                                         String requestId,
                                         String bizRefType,
                                         Long bizRefId) {
        return InventoryTxn.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .itemId(itemId)
                .locationId(locationId)
                .qty(qty)
                .beforeTotal(beforeTotal)
                .afterTotal(afterTotal)
                .beforeLocked(beforeLocked)
                .afterLocked(afterLocked)
                .lockId(lockId)
                .requestId(requestId)
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .txnType(InventoryTxnType.DEDUCT)
                .txnDirection(InventoryTxnDirection.OUT)
                .build();
    }

    public static InventoryTxn forAdjust(Long tenantId,
                                         Long storeId,
                                         Long itemId,
                                         Long locationId,
                                         Long qty,
                                         Long beforeTotal,
                                         Long afterTotal,
                                         Long beforeLocked,
                                         Long afterLocked,
                                         InventoryTxnDirection direction,
                                         String requestId,
                                         String bizRefType,
                                         Long bizRefId) {
        return InventoryTxn.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .itemId(itemId)
                .locationId(locationId)
                .qty(qty)
                .beforeTotal(beforeTotal)
                .afterTotal(afterTotal)
                .beforeLocked(beforeLocked)
                .afterLocked(afterLocked)
                .requestId(requestId)
                .bizRefType(bizRefType)
                .bizRefId(bizRefId)
                .txnType(InventoryTxnType.ADJUST)
                .txnDirection(direction)
                .build();
    }
}
