package com.bluecone.app.order.infra.gateway;

import com.bluecone.app.order.domain.gateway.PaymentRefundGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mock 支付退款网关实现。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>M4 阶段的 Mock 实现，直接返回成功</li>
 *   <li>用于测试和验证退款流程</li>
 *   <li>后续替换为真实的微信/支付宝退款接口实现</li>
 * </ul>
 * 
 * <h3>Mock 策略：</h3>
 * <ul>
 *   <li>直接返回成功响应，模拟退款成功</li>
 *   <li>生成随机的第三方退款单号（mock_refund_xxx）</li>
 *   <li>记录日志，方便调试和追踪</li>
 * </ul>
 */
@Slf4j
@Component
public class MockPaymentRefundGateway implements PaymentRefundGateway {
    
    /**
     * 发起退款请求（Mock 实现，直接返回成功）。
     * 
     * @param request 退款请求参数
     * @return 退款响应结果（总是成功）
     */
    @Override
    public RefundResponse refund(RefundRequest request) {
        log.info("【Mock退款】发起退款请求：refundId={}, orderNo={}, channel={}, refundAmount={}, payNo={}, refundReason={}",
                request.getRefundId(),
                request.getOrderNo(),
                request.getChannel() != null ? request.getChannel().getCode() : "NULL",
                request.getRefundAmount(),
                request.getPayNo(),
                request.getRefundReason());
        
        // Mock 实现：直接返回成功，生成随机退款单号
        String mockRefundNo = "mock_refund_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        log.info("【Mock退款】退款成功：refundId={}, orderNo={}, mockRefundNo={}", 
                request.getRefundId(), request.getOrderNo(), mockRefundNo);
        
        return RefundResponse.success(mockRefundNo);
    }
}
