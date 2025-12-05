package com.bluecone.app.inventory.domain.model;

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
public class InventoryStock implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private Long totalQty;

    private Long lockedQty;

    private Long availableQty;

    private Long safetyStock;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean canLock(long lockQty, InventoryPolicy policy) {
        if (lockQty < 0) {
            return false;
        }
        long available = getOrZero(this.availableQty);
        return available >= lockQty;
    }

    public void applyLock(long lockQty) {
        if (lockQty < 0) {
            throw new IllegalArgumentException("lockQty must be non-negative");
        }
        long available = getOrZero(this.availableQty);
        long locked = getOrZero(this.lockedQty);
        long newAvailable = available - lockQty;
        if (newAvailable < 0) {
            throw new IllegalArgumentException("insufficient available qty");
        }
        this.availableQty = newAvailable;
        this.lockedQty = locked + lockQty;
    }

    public void applyDeduct(long qty) {
        if (qty < 0) {
            throw new IllegalArgumentException("deduct qty must be non-negative");
        }
        long total = getOrZero(this.totalQty);
        long locked = getOrZero(this.lockedQty);
        if (qty > locked) {
            throw new IllegalArgumentException("locked qty insufficient");
        }
        if (qty > total) {
            throw new IllegalArgumentException("total qty insufficient");
        }
        long newTotal = total - qty;
        long newLocked = locked - qty;
        long newAvailable = newTotal - newLocked;
        if (newAvailable < 0) {
            throw new IllegalArgumentException("available qty would be negative");
        }
        this.totalQty = newTotal;
        this.lockedQty = newLocked;
        this.availableQty = newAvailable;
    }

    public void applyRelease(long qty) {
        if (qty < 0) {
            throw new IllegalArgumentException("release qty must be non-negative");
        }
        long locked = getOrZero(this.lockedQty);
        if (qty > locked) {
            throw new IllegalArgumentException("locked qty insufficient for release");
        }
        long available = getOrZero(this.availableQty);
        this.lockedQty = locked - qty;
        this.availableQty = available + qty;
    }

    private long getOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
