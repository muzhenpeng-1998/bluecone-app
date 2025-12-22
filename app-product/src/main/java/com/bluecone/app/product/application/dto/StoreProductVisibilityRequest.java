package com.bluecone.app.product.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 门店商品可见性设置请求
 * 
 * <p>用于控制商品在指定门店的上架/下架状态。
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreProductVisibilityRequest {
    
    /**
     * 租户ID（必填）
     */
    @NotNull(message = "租户ID不能为空")
    private Long tenantId;
    
    /**
     * 操作人ID（必填）
     */
    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;
    
    /**
     * 是否可见（必填）
     * true=上架，false=下架
     */
    @NotNull(message = "可见性不能为空")
    private Boolean visible;
    
    /**
     * 售卖渠道（可选，默认 ALL）
     * 可选值：ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP
     */
    private String channel;
    
    /**
     * SKU ID（可选）
     * 如果指定，则只控制该 SKU 的可见性
     * 如果不指定，则控制整个 SPU 的可见性
     */
    private Long skuId;
    
    /**
     * 展示开始时间（可选）
     * 定时上架：设置未来时间
     */
    private LocalDateTime displayStartAt;
    
    /**
     * 展示结束时间（可选）
     * 定时下架：设置未来时间
     */
    private LocalDateTime displayEndAt;
    
    /**
     * 是否自动重建快照（可选，默认 false）
     * true=操作后立即重建门店菜单快照
     * false=仅失效缓存，不重建快照
     */
    private Boolean autoRebuildSnapshot;
}

