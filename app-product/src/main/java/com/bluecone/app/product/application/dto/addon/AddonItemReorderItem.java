package com.bluecone.app.product.application.dto.addon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小料项排序项
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonItemReorderItem {
    
    /**
     * 小料项ID
     */
    private Long itemId;
    
    /**
     * 新的排序值
     */
    private Integer sortOrder;
}

