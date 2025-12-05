package com.bluecone.app.inventory.domain.model;

import com.bluecone.app.inventory.domain.type.InventoryDeductMode;
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
public class InventoryPolicy implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private InventoryDeductMode deductMode;

    private Boolean oversellAllowed;

    private Long oversellLimit;

    private Long maxDailySold;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }
}
