package com.bluecone.app.order.controller;

import com.bluecone.app.core.log.annotation.ApiLog;
import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.order.application.RefundAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 退款回调 Controller（M4 Mock 版本）。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>接收第三方支付渠道的退款回调通知（M4 使用 Mock 实现）</li>
 *   <li>解析回调报文并调用 RefundAppService 处理</li>
 *   <li>返回统一的响应格式</li>
 * </ul>
 * 
 * <h3>实现说明：</h3>
 * <ul>
 *   <li>M4 阶段：Mock 实现，接收简化的 JSON 请求</li>
 *   <li>后续扩展：解析真实的微信/支付宝回调报文（XML/JSON）</li>
 *   <li>后续扩展：验证签名、幂等性检查</li>
 * </ul>
 * 
 * <p>注意：此接口已通过路径 /notify 被 ApiResponseAdvice 自动排除包装</p>
 */
@RestController
@RequestMapping("/api/pay/refund")
@RequiredArgsConstructor
@Slf4j
public class RefundCallbackController {
    
    private final RefundAppService refundAppService;
    
    /**
     * Mock 退款回调接口（M4 版本）。
     * 
     * <h4>接口说明：</h4>
     * <ul>
     *   <li>M4 阶段：接收简化的 JSON 请求，用于测试退款回调流程</li>
     *   <li>后续扩展：解析真实的微信/支付宝回调报文</li>
     * </ul>
     * 
     * @param request Mock 退款回调请求
     * @return 统一响应格式
     */
    @PostMapping("/notify")
    @ApiLog("退款回调通知")
    public ApiResponse<Void> onRefundNotify(@RequestBody MockRefundNotifyRequest request) {
        log.info("收到 Mock 退款回调通知：notifyId={}, refundId={}, refundNo={}, success={}",
                request.getNotifyId(), request.getRefundId(), request.getRefundNo(), request.getSuccess());
        
        // 调用 RefundAppService 处理回调
        refundAppService.onRefundNotify(
                request.getNotifyId(),
                request.getRefundId(),
                request.getRefundNo(),
                request.getSuccess(),
                request.getErrorMsg()
        );
        
        log.info("退款回调处理完成：notifyId={}, refundId={}", request.getNotifyId(), request.getRefundId());
        
        return ApiResponse.success();
    }
    
    /**
     * Mock 退款回调请求（M4 版本）。
     */
    @lombok.Data
    public static class MockRefundNotifyRequest {
        /**
         * 通知ID（幂等键）。
         */
        private String notifyId;
        
        /**
         * 退款单号（rfd_xxx）。
         */
        private String refundId;
        
        /**
         * 第三方退款单号（如微信退款单号）。
         */
        private String refundNo;
        
        /**
         * 是否成功。
         */
        private Boolean success;
        
        /**
         * 失败原因（成功时为 null）。
         */
        private String errorMsg;
    }
}
