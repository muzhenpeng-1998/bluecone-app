package com.bluecone.app.product.application.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 更新商品分类命令
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductCategoryCommand {
    
    /**
     * 分类名称
     */
    private String title;
    
    /**
     * 分类图标URL
     */
    private String imageUrl;
    
    /**
     * 排序值（数值越大越靠前）
     */
    private Integer sortOrder;
    
    /**
     * 是否启用（true=显示，false=隐藏）
     */
    private Boolean enabled;
    
    /**
     * 定时展示开始时间（null表示立即生效）
     */
    private LocalDateTime displayStartAt;
    
    /**
     * 定时展示结束时间（null表示永久有效）
     */
    private LocalDateTime displayEndAt;
}

