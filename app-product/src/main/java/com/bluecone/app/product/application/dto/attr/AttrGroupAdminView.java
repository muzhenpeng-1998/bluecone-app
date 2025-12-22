package com.bluecone.app.product.application.dto.attr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 属性组管理视图
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttrGroupAdminView {
    
    /**
     * 属性组ID
     */
    private Long id;
    
    /**
     * 属性组名称
     */
    private String title;
    
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
}

