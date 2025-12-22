package com.bluecone.app.product.application.dto.addon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 更新小料项命令
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAddonItemCommand {
    
    /**
     * 小料项名称
     */
    private String title;
    
    /**
     * 价格增量
     */
    private BigDecimal priceDelta;
    
    /**
     * 排序值
     */
    private Integer sortOrder;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 定时展示开始时间
     */
    private LocalDateTime displayStartAt;
    
    /**
     * 定时展示结束时间
     */
    private LocalDateTime displayEndAt;
}

