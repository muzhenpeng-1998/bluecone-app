package com.bluecone.app.order.domain.gateway;

import com.bluecone.app.order.domain.enums.RefundChannel;

import java.math.BigDecimal;

/**
 * 支付退款网关接口。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>向第三方支付渠道发起退款请求</li>
 *   <li>封装不同支付渠道的退款接口差异</li>
 *   <li>返回统一的退款响应结果</li>
 * </ul>
 * 
 * <h3>实现要求：</h3>
 * <ul>
 *   <li>M4 阶段先提供 Mock 实现，直接返回成功</li>
 *   <li>后续扩展时实现真实的微信/支付宝退款接口</li>
 *   <li>需要保证幂等性（同一退款单多次调用，只退款一次）</li>
 * </ul>
 */
public interface PaymentRefundGateway {
    
    /**
     * 发起退款请求。
     * 
     * @param request 退款请求参数
     * @return 退款响应结果
     */
    RefundResponse refund(RefundRequest request);
    
    /**
     * 退款请求参数。
     */
    class RefundRequest {
        /**
         * 退款单号（对外展示，PublicId格式：rfd_xxx）。
         */
        private String refundId;
        
        /**
         * 订单号（对外展示，PublicId格式：ord_xxx）。
         */
        private String orderNo;
        
        /**
         * 退款渠道（WECHAT、ALIPAY、MOCK）。
         */
        private RefundChannel channel;
        
        /**
         * 退款金额（实际退款金额，单位：元）。
         */
        private BigDecimal refundAmount;
        
        /**
         * 订单金额（原订单金额，单位：元）。
         */
        private BigDecimal orderAmount;
        
        /**
         * 第三方支付单号（如微信transaction_id，用于退款时传给支付网关）。
         */
        private String payNo;
        
        /**
         * 退款原因描述。
         */
        private String refundReason;
        
        // Getters and Setters
        
        public String getRefundId() {
            return refundId;
        }
        
        public void setRefundId(String refundId) {
            this.refundId = refundId;
        }
        
        public String getOrderNo() {
            return orderNo;
        }
        
        public void setOrderNo(String orderNo) {
            this.orderNo = orderNo;
        }
        
        public RefundChannel getChannel() {
            return channel;
        }
        
        public void setChannel(RefundChannel channel) {
            this.channel = channel;
        }
        
        public BigDecimal getRefundAmount() {
            return refundAmount;
        }
        
        public void setRefundAmount(BigDecimal refundAmount) {
            this.refundAmount = refundAmount;
        }
        
        public BigDecimal getOrderAmount() {
            return orderAmount;
        }
        
        public void setOrderAmount(BigDecimal orderAmount) {
            this.orderAmount = orderAmount;
        }
        
        public String getPayNo() {
            return payNo;
        }
        
        public void setPayNo(String payNo) {
            this.payNo = payNo;
        }
        
        public String getRefundReason() {
            return refundReason;
        }
        
        public void setRefundReason(String refundReason) {
            this.refundReason = refundReason;
        }
    }
    
    /**
     * 退款响应结果。
     */
    class RefundResponse {
        /**
         * 是否成功。
         */
        private boolean success;
        
        /**
         * 第三方退款单号（如微信退款单号）。
         */
        private String refundNo;
        
        /**
         * 错误码。
         */
        private String errorCode;
        
        /**
         * 错误消息。
         */
        private String errorMsg;
        
        // Getters and Setters
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public String getRefundNo() {
            return refundNo;
        }
        
        public void setRefundNo(String refundNo) {
            this.refundNo = refundNo;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }
        
        public String getErrorMsg() {
            return errorMsg;
        }
        
        public void setErrorMsg(String errorMsg) {
            this.errorMsg = errorMsg;
        }
        
        /**
         * 创建成功响应。
         * 
         * @param refundNo 第三方退款单号
         * @return 成功响应
         */
        public static RefundResponse success(String refundNo) {
            RefundResponse response = new RefundResponse();
            response.setSuccess(true);
            response.setRefundNo(refundNo);
            return response;
        }
        
        /**
         * 创建失败响应。
         * 
         * @param errorCode 错误码
         * @param errorMsg 错误消息
         * @return 失败响应
         */
        public static RefundResponse failure(String errorCode, String errorMsg) {
            RefundResponse response = new RefundResponse();
            response.setSuccess(false);
            response.setErrorCode(errorCode);
            response.setErrorMsg(errorMsg);
            return response;
        }
    }
}
