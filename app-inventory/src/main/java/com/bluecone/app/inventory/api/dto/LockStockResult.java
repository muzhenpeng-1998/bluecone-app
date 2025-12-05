package com.bluecone.app.inventory.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockStockResult {

    private boolean success;

    private String code;

    private String message;

    private Long lockId;

    private StockView currentStock;

    public static LockStockResult ok(Long lockId, StockView stockView) {
        return LockStockResult.builder()
                .success(true)
                .code("OK")
                .message("锁定库存成功")
                .lockId(lockId)
                .currentStock(stockView)
                .build();
    }

    public static LockStockResult failed(String code, String message) {
        return LockStockResult.builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
}
