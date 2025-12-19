package com.bluecone.app.wallet.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 充值创建结果
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RechargeCreateResult {
    
    /**
     * 充值单号
     */
    private String rechargeNo;
    
    /**
     * 充值金额（单位：元）
     */
    private BigDecimal rechargeAmount;
    
    /**
     * 赠送金额（单位：元）
     */
    private BigDecimal bonusAmount;
    
    /**
     * 总到账金额（单位：元）
     */
    private BigDecimal totalAmount;
    
    /**
     * 支付单ID
     */
    private Long payOrderId;
    
    /**
     * 支付渠道
     */
    private String payChannel;
    
    /**
     * 支付参数（用于拉起支付）
     * 微信小程序支付：timeStamp、nonceStr、package、signType、paySign
     */
    private Map<String, String> payParams;
    
    /**
     * 充值单状态
     */
    private String status;
}
