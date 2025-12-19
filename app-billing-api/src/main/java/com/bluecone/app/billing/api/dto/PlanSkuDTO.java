package com.bluecone.app.billing.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 套餐 SKU DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanSkuDTO {
    
    private Long id;
    
    private String planCode;
    private String planName;
    private Integer planLevel;
    
    private String billingPeriod;
    private Integer periodMonths;
    
    private Long priceFen;
    private Long originalPriceFen;
    
    private Map<String, Object> features;
    
    private String status;
    private Integer sortOrder;
}
