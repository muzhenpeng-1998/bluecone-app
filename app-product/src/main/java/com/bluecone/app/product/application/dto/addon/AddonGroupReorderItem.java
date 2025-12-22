package com.bluecone.app.product.application.dto.addon;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小料组排序项
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonGroupReorderItem {
    
    /**
     * 小料组ID
     */
    private Long groupId;
    
    /**
     * 新的排序值
     */
    private Integer sortOrder;
}

