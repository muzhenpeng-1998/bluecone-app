package com.bluecone.app.product.application.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商品分类管理视图
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryAdminView {
    
    /**
     * 分类ID
     */
    private Long id;
    
    /**
     * 父分类ID（0表示顶级分类）
     */
    private Long parentId;
    
    /**
     * 分类名称
     */
    private String title;
    
    /**
     * 分类图标URL
     */
    private String imageUrl;
    
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
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 分类层级
     */
    private Integer level;
}

