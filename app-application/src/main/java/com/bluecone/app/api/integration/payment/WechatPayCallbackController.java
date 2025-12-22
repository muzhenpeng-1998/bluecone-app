package com.bluecone.app.application.payment;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.payment.api.WechatPayCallbackCommand;
import com.bluecone.app.payment.application.WechatPayCallbackApplicationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信支付异步通知 HTTP 入口。
 * <p>
 * 仅负责：
 * - 接收微信回调 JSON（假设已解密）；
 * - 解析为业务命令对象后调用应用服务；
 * - 按微信要求返回 SUCCESS/FAIL。
 */
@Tag(name = "🔌 第三方集成 > 支付相关 > 微信支付回调", description = "微信支付回调接口")
@RestController
@RequestMapping("/open-api/wechat/pay")
@NoApiResponseWrap
public class WechatPayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatPayCallbackController.class);

    private final ObjectMapper objectMapper;
    private final WechatPayCallbackApplicationService callbackService;

    public WechatPayCallbackController(ObjectMapper objectMapper,
                                       WechatPayCallbackApplicationService callbackService) {
        this.objectMapper = objectMapper;
        this.callbackService = callbackService;
    }

    /**
     * 微信支付异步通知。
     * <p>
     * 路径需与微信商户平台配置的 notify_url 保持一致。
     */
    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> payNotify(@RequestBody String body) {
        try {
            WechatPayCallbackCommand command = parseCallback(body);
            callbackService.handleWechatPayCallback(command);

            Map<String, String> resp = new HashMap<>();
            resp.put("code", "SUCCESS");
            resp.put("message", "成功");
            return ResponseEntity.ok(resp);
        } catch (BusinessException ex) {
            log.warn("[WechatPay] 业务处理失败: {}", ex.getMessage(), ex);
            Map<String, String> resp = new HashMap<>();
            resp.put("code", "FAIL");
            resp.put("message", "业务处理失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        } catch (Exception ex) {
            log.error("[WechatPay] 回调处理异常", ex);
            Map<String, String> resp = new HashMap<>();
            resp.put("code", "FAIL");
            resp.put("message", "系统异常");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * 将微信回调 JSON 字符串解析为命令对象。
     * <p>
     * 当前仅支持“已解密的交易 JSON”，生产环境请接入官方 SDK 做验签和解密。
     */
    private WechatPayCallbackCommand parseCallback(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            String appId = getText(root, "appid");
            String mchId = getText(root, "mchid");
            String outTradeNo = getText(root, "out_trade_no");
            String transactionId = getText(root, "transaction_id");
            String tradeState = getText(root, "trade_state");
            String bankType = getText(root, "bank_type");
            String attach = getText(root, "attach");
            String notifyId = getText(root, "id");

            Long total = null;
            JsonNode amountNode = root.get("amount");
            if (amountNode != null && amountNode.has("total")) {
                total = amountNode.get("total").asLong();
            }

            Instant successTime = null;
            String successTimeStr = getText(root, "success_time");
            if (successTimeStr != null) {
                try {
                    successTime = OffsetDateTime.parse(successTimeStr).toInstant();
                } catch (Exception e) {
                    // 忽略解析失败，保持 null
                }
            }

            WechatPayCallbackCommand cmd = new WechatPayCallbackCommand();
            cmd.setRawBody(body);
            cmd.setAppId(appId);
            cmd.setMchId(mchId);
            cmd.setOutTradeNo(outTradeNo);
            cmd.setTransactionId(transactionId);
            cmd.setTradeState(tradeState);
            cmd.setBankType(bankType);
            cmd.setAttach(attach);
            cmd.setNotifyId(notifyId);
            cmd.setTotalAmount(total);
            cmd.setSuccessTime(successTime);

            return cmd;
        } catch (Exception e) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "非法的微信支付回调报文", e);
        }
    }

    private String getText(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }
}
