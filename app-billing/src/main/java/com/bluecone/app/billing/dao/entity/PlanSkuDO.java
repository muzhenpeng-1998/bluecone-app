package com.bluecone.app.billing.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 套餐 SKU 实体
 */
@Data
@TableName("bc_plan_sku")
public class PlanSkuDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String planCode;
    private String planName;
    private Integer planLevel;
    
    private String billingPeriod;
    private Integer periodMonths;
    
    private Long priceFen;
    private Long originalPriceFen;
    
    private String features;
    
    private String status;
    private Integer sortOrder;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
