package com.bluecone.app.product.application.dto.attr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 属性组排序项
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttrGroupReorderItem {
    
    /**
     * 属性组ID
     */
    private Long groupId;
    
    /**
     * 新的排序值
     */
    private Integer sortOrder;
}

