package com.bluecone.app.order.api.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小程序用户侧确认订单预览响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmOrderPreviewResponse {

    private boolean canPlaceOrder;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String currency;

    private Integer expectedReadyTimeSeconds;

    private String storeOpenStatus;

    private String message;

    private Integer sessionVersion;

    private String ext;
    
    /**
     * 钱包余额信息（M5 新增）
     */
    private WalletBalanceInfo walletBalance;
    
    /**
     * 钱包余额信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WalletBalanceInfo {
        /**
         * 可用余额
         */
        private BigDecimal availableBalance;
        
        /**
         * 冻结余额
         */
        private BigDecimal frozenBalance;
        
        /**
         * 总余额（可用+冻结）
         */
        private BigDecimal totalBalance;
        
        /**
         * 余额是否足够支付本订单
         */
        private Boolean sufficient;
        
        /**
         * 币种
         */
        private String currency;
    }
}
