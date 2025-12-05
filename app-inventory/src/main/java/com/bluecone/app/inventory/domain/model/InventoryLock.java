package com.bluecone.app.inventory.domain.model;

import com.bluecone.app.inventory.domain.type.InventoryLockStatus;
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
public class InventoryLock implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private Long orderId;

    private Long orderItemId;

    private Long lockQty;

    private InventoryLockStatus status;

    private LocalDateTime expireAt;

    private String requestId;

    private String remark;

    private String ext;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static InventoryLock createNew(Long tenantId,
                                          Long storeId,
                                          Long itemId,
                                          Long locationId,
                                          Long orderId,
                                          Long orderItemId,
                                          Long lockQty,
                                          String requestId,
                                          LocalDateTime expireAt) {
        return InventoryLock.builder()
                .tenantId(tenantId)
                .storeId(storeId)
                .itemId(itemId)
                .locationId(locationId)
                .orderId(orderId)
                .orderItemId(orderItemId)
                .lockQty(lockQty)
                .requestId(requestId)
                .expireAt(expireAt)
                .status(InventoryLockStatus.LOCKED)
                .build();
    }

    public void markConfirmed() {
        this.status = InventoryLockStatus.CONFIRMED;
    }

    public void markReleased() {
        this.status = InventoryLockStatus.RELEASED;
    }

    public void markExpired() {
        this.status = InventoryLockStatus.EXPIRED;
    }
}
