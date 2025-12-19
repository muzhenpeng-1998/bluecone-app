package com.bluecone.app.billing.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订阅账单实体
 */
@Data
@TableName("bc_billing_invoice")
public class BillingInvoiceDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String invoiceNo;
    private String idempotencyKey;
    
    private Long planSkuId;
    private String planCode;
    private String planName;
    private String billingPeriod;
    private Integer periodMonths;
    
    private Long amountFen;
    private Long paidAmountFen;
    
    private String paymentChannel;
    private String channelTradeNo;
    private LocalDateTime paidAt;
    
    private String status;
    
    private LocalDateTime effectiveStartAt;
    private LocalDateTime effectiveEndAt;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
