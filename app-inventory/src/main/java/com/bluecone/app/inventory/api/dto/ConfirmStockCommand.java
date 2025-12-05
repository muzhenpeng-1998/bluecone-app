package com.bluecone.app.inventory.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmStockCommand {

    private Long tenantId;

    private Long storeId;

    private Long orderId;

    /**
     * 可选：如果只想确认指定锁记录，可传 lockIds。
     * 如果为空，则按 tenantId+storeId+orderId 查询所有 LOCKED 记录。
     */
    private List<Long> lockIds;

    /**
     * 本批次扣减操作的幂等 ID。
     */
    private String requestId;
}
