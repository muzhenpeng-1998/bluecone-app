package com.bluecone.app.inventory.domain.model;

import com.bluecone.app.inventory.domain.type.InventoryItemType;
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
public class InventoryItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private InventoryItemType itemType;

    private Long refId;

    private String externalCode;

    private String name;

    private String unit;

    private Integer status;

    private String remark;

    private String ext;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean isEnabled() {
        return Integer.valueOf(1).equals(status);
    }

    public void enable() {
        this.status = 1;
    }

    public void disable() {
        this.status = 0;
    }
}
