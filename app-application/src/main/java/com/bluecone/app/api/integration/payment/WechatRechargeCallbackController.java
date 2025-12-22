package com.bluecone.app.application.payment;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.wallet.api.facade.WalletRechargeFacade;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信充值支付回调控制器
 * 处理微信支付充值回调通知
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Tag(name = "🔌 第三方集成 > 支付相关 > 微信充值回调", description = "微信充值回调接口")
@Slf4j
@RestController
@RequestMapping("/open-api/wechat/recharge")
@NoApiResponseWrap
public class WechatRechargeCallbackController {
    
    private final ObjectMapper objectMapper;
    private final WalletRechargeFacade walletRechargeFacade;

    public WechatRechargeCallbackController(@Qualifier("redisObjectMapper") ObjectMapper objectMapper,
                                           WalletRechargeFacade walletRechargeFacade) {
        this.objectMapper = objectMapper;
        this.walletRechargeFacade = walletRechargeFacade;
    }
    
    /**
     * 微信充值支付异步通知
     * 
     * 路径需与微信商户平台配置的 notify_url 保持一致
     * 
     * @param body 微信回调 JSON（假设已解密）
     * @return 微信要求的响应格式
     */
    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> rechargeNotify(@RequestBody String body) {
        log.info("[WechatRechargeCallback] 收到微信充值回调：body={}", body);
        
        try {
            // 1. 解析微信回调
            RechargeCallbackData callbackData = parseCallback(body);
            
            log.info("[WechatRechargeCallback] 解析回调成功：transactionId={}, outTradeNo={}, tradeState={}", 
                    callbackData.getTransactionId(), callbackData.getOutTradeNo(), callbackData.getTradeState());
            
            // 2. 检查交易状态
            if (!"SUCCESS".equals(callbackData.getTradeState())) {
                log.warn("[WechatRechargeCallback] 交易状态非成功：tradeState={}, transactionId={}", 
                        callbackData.getTradeState(), callbackData.getTransactionId());
                return buildSuccessResponse(); // 仍然返回 SUCCESS，避免微信重复回调
            }
            
            // 3. 调用充值门面处理回调（幂等）
            walletRechargeFacade.onRechargePaid(
                    callbackData.getTenantId(),
                    callbackData.getTransactionId(), // 使用微信交易号作为渠道交易号
                    callbackData.getSuccessTime()
            );
            
            log.info("[WechatRechargeCallback] 充值回调处理成功：transactionId={}", 
                    callbackData.getTransactionId());
            
            return buildSuccessResponse();
            
        } catch (BusinessException ex) {
            log.warn("[WechatRechargeCallback] 业务处理失败: {}", ex.getMessage(), ex);
            return buildFailResponse("业务处理失败");
        } catch (Exception ex) {
            log.error("[WechatRechargeCallback] 回调处理异常", ex);
            return buildFailResponse("系统异常");
        }
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 解析微信回调 JSON
     * 
     * 当前仅支持"已解密的交易 JSON"，生产环境请接入官方 SDK 做验签和解密
     */
    private RechargeCallbackData parseCallback(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            
            String appId = getText(root, "appid");
            String mchId = getText(root, "mchid");
            String outTradeNo = getText(root, "out_trade_no"); // 商户订单号（充值单号）
            String transactionId = getText(root, "transaction_id"); // 微信支付单号
            String tradeState = getText(root, "trade_state"); // 交易状态：SUCCESS/REFUND/NOTPAY/CLOSED/REVOKED/USERPAYING/PAYERROR
            String bankType = getText(root, "bank_type");
            String attach = getText(root, "attach"); // 附加数据（可用于传递租户ID等）
            
            // 解析金额
            Long totalAmount = null;
            JsonNode amountNode = root.get("amount");
            if (amountNode != null && amountNode.has("total")) {
                totalAmount = amountNode.get("total").asLong(); // 单位：分
            }
            
            // 解析支付完成时间
            LocalDateTime successTime = null;
            String successTimeStr = getText(root, "success_time");
            if (successTimeStr != null) {
                try {
                    Instant instant = OffsetDateTime.parse(successTimeStr).toInstant();
                    successTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                } catch (Exception e) {
                    log.warn("[WechatRechargeCallback] 解析支付时间失败：successTimeStr={}", successTimeStr, e);
                    successTime = LocalDateTime.now();
                }
            } else {
                successTime = LocalDateTime.now();
            }
            
            // 解析租户ID（从 attach 字段）
            Long tenantId = 1L; // 默认租户
            if (attach != null && !attach.isEmpty()) {
                try {
                    JsonNode attachNode = objectMapper.readTree(attach);
                    if (attachNode.has("tenantId")) {
                        tenantId = attachNode.get("tenantId").asLong();
                    }
                } catch (Exception e) {
                    log.warn("[WechatRechargeCallback] 解析 attach 失败：attach={}", attach, e);
                }
            }
            
            return RechargeCallbackData.builder()
                    .appId(appId)
                    .mchId(mchId)
                    .outTradeNo(outTradeNo)
                    .transactionId(transactionId)
                    .tradeState(tradeState)
                    .bankType(bankType)
                    .totalAmount(totalAmount)
                    .successTime(successTime)
                    .tenantId(tenantId)
                    .build();
            
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "非法的微信支付回调报文", e);
        }
    }
    
    private String getText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }
    
    private ResponseEntity<Map<String, String>> buildSuccessResponse() {
        Map<String, String> resp = new HashMap<>();
        resp.put("code", "SUCCESS");
        resp.put("message", "成功");
        return ResponseEntity.ok(resp);
    }
    
    private ResponseEntity<Map<String, String>> buildFailResponse(String message) {
        Map<String, String> resp = new HashMap<>();
        resp.put("code", "FAIL");
        resp.put("message", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
    }
    
    // ========== 内部数据类 ==========
    
    @lombok.Data
    @lombok.Builder
    private static class RechargeCallbackData {
        private String appId;
        private String mchId;
        private String outTradeNo; // 商户订单号（充值单号）
        private String transactionId; // 微信支付单号
        private String tradeState; // 交易状态
        private String bankType;
        private Long totalAmount; // 单位：分
        private LocalDateTime successTime;
        private Long tenantId;
    }
}
