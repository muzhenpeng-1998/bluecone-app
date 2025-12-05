package com.bluecone.app.order.domain.model;

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
public class OrderSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private Long storeId;

    private String sessionId;

    private Long tableId;

    private String status;

    @Builder.Default
    private Integer version = 0;

    private String lastSnapshot;

    private String extJson;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }

    public void markConfirmed() {
        this.status = "CONFIRMED";
    }

    public void markClosed() {
        this.status = "CLOSED";
    }
}
