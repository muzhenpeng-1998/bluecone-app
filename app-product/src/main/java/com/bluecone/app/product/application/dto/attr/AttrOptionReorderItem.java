package com.bluecone.app.product.application.dto.attr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 属性选项排序项
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttrOptionReorderItem {
    
    /**
     * 属性选项ID
     */
    private Long optionId;
    
    /**
     * 新的排序值
     */
    private Integer sortOrder;
}

