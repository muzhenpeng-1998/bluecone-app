package com.bluecone.app.application.payment;

import com.bluecone.app.api.advice.NoApiResponseWrap;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.payment.api.WechatPayCallbackCommand;
import com.bluecone.app.payment.application.WechatPayCallbackApplicationService;
import com.github.binarywang.wxpay.bean.notify.SignatureHeader;
import com.github.binarywang.wxpay.bean.notify.WxPayPartnerNotifyV3Result;
import com.github.binarywang.wxpay.exception.WxPayException;
import com.github.binarywang.wxpay.service.WxPayService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付 V3 服务商模式异步通知 HTTP 入口。
 * <p>
 * 负责：
 * - 接收微信 V3 回调原始报文和签名头；
 * - 使用 WxPayService 进行验签和解密；
 * - 解析服务商模式回调结构（partner notify）；
 * - 调用应用服务处理业务逻辑；
 * - 按微信要求返回 SUCCESS/FAIL。
 * </p>
 */
@Tag(name = "🔌 第三方集成 > 支付相关 > 微信支付回调", description = "微信支付回调接口")
@RestController
@RequestMapping("/open-api/wechat/pay")
@NoApiResponseWrap
public class WechatPayCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WechatPayCallbackController.class);

    private final WechatPayCallbackApplicationService callbackService;

    @Autowired(required = false)
    private WxPayService wxPayService;

    public WechatPayCallbackController(WechatPayCallbackApplicationService callbackService) {
        this.callbackService = callbackService;
    }

    /**
     * 微信支付 V3 服务商模式异步通知。
     * <p>
     * 路径需与微信商户平台配置的 notify_url 保持一致。
     * </p>
     *
     * @param wechatpayTimestamp  微信签名时间戳（HTTP 头）
     * @param wechatpayNonce      微信签名随机串（HTTP 头）
     * @param wechatpaySignature  微信签名值（HTTP 头）
     * @param wechatpaySerial     微信平台证书序列号（HTTP 头）
     * @param body                原始请求体（加密报文）
     * @return 微信要求的响应格式
     */
    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> payNotify(
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String wechatpayTimestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String wechatpayNonce,
            @RequestHeader(value = "Wechatpay-Signature", required = false) String wechatpaySignature,
            @RequestHeader(value = "Wechatpay-Serial", required = false) String wechatpaySerial,
            @RequestBody String body) {

        log.info("[WechatPayCallback] 收到微信支付回调，timestamp={}, nonce={}, serial={}",
                wechatpayTimestamp, wechatpayNonce, wechatpaySerial);

        try {
            // 1. 验签并解密（使用 WxJava）
            WechatPayCallbackCommand command = parseAndVerifyCallback(
                    body, wechatpayTimestamp, wechatpayNonce, wechatpaySignature, wechatpaySerial);

            // 2. 调用业务服务处理
            callbackService.handleWechatPayCallback(command);

            // 3. 返回成功响应
            Map<String, String> resp = new HashMap<>();
            resp.put("code", "SUCCESS");
            resp.put("message", "成功");
            return ResponseEntity.ok(resp);

        } catch (BusinessException ex) {
            log.warn("[WechatPayCallback] 业务处理失败: {}", ex.getMessage(), ex);
            Map<String, String> resp = new HashMap<>();
            resp.put("code", "FAIL");
            resp.put("message", "业务处理失败");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        } catch (Exception ex) {
            log.error("[WechatPayCallback] 回调处理异常", ex);
            Map<String, String> resp = new HashMap<>();
            resp.put("code", "FAIL");
            resp.put("message", "系统异常");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
        }
    }

    /**
     * 验签并解密微信 V3 服务商回调报文。
     *
     * @param body               原始加密报文
     * @param timestamp          签名时间戳
     * @param nonce              签名随机串
     * @param signature          签名值
     * @param serial             平台证书序列号
     * @return 解析后的回调命令对象
     */
    private WechatPayCallbackCommand parseAndVerifyCallback(String body, String timestamp,
                                                            String nonce, String signature, String serial) {
        if (wxPayService == null) {
            log.warn("[WechatPayCallback] WxPayService 未启用，跳过验签解密（仅用于本地开发）");
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "微信支付服务未启用，无法处理回调");
        }

        try {
            // 构造签名头
            SignatureHeader signatureHeader = new SignatureHeader();
            signatureHeader.setTimeStamp(timestamp);
            signatureHeader.setNonce(nonce);
            signatureHeader.setSignature(signature);
            signatureHeader.setSerial(serial);

            // 调用 WxJava 进行验签和解密（服务商模式）
            WxPayPartnerNotifyV3Result notifyResult = wxPayService.parsePartnerOrderNotifyV3Result(body, signatureHeader);

            log.info("[WechatPayCallback] 验签解密成功，outTradeNo={}, transactionId={}, tradeState={}",
                    notifyResult.getResult().getOutTradeNo(),
                    notifyResult.getResult().getTransactionId(),
                    notifyResult.getResult().getTradeState());

            // 转换为业务命令对象
            return convertToCommand(notifyResult, body);

        } catch (WxPayException e) {
            log.error("[WechatPayCallback] 验签或解密失败，errCode={}, errMsg={}",
                    e.getErrCode(), e.getErrCodeDes(), e);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR,
                    "微信支付回调验签失败: " + e.getErrCodeDes());
        }
    }

    /**
     * 将 WxPayPartnerNotifyV3Result 转换为 WechatPayCallbackCommand。
     */
    private WechatPayCallbackCommand convertToCommand(WxPayPartnerNotifyV3Result notifyResult, String rawBody) {
        WxPayPartnerNotifyV3Result.DecryptNotifyResult result = notifyResult.getResult();

        WechatPayCallbackCommand cmd = new WechatPayCallbackCommand();
        cmd.setRawBody(rawBody);

        // 服务商信息
        cmd.setAppId(result.getSpAppid());
        cmd.setMchId(result.getSpMchid());

        // 子商户信息
        cmd.setSubAppId(result.getSubAppid());
        cmd.setSubMchId(result.getSubMchid());

        // 订单信息
        cmd.setOutTradeNo(result.getOutTradeNo());
        cmd.setTransactionId(result.getTransactionId());
        cmd.setTradeState(result.getTradeState());
        cmd.setBankType(result.getBankType());
        cmd.setAttach(result.getAttach());

        // 金额信息
        if (result.getAmount() != null) {
            cmd.setTotalAmount(Long.valueOf(result.getAmount().getTotal()));
        }

        // 支付者信息
        if (result.getPayer() != null) {
            cmd.setPayerOpenId(result.getPayer().getSubOpenid());
        }

        // 支付成功时间
        if (result.getSuccessTime() != null) {
            try {
                // 微信返回格式：2018-06-08T10:34:56+08:00
                cmd.setSuccessTime(OffsetDateTime.parse(result.getSuccessTime(),
                        DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
            } catch (Exception e) {
                log.warn("[WechatPayCallback] 解析支付成功时间失败，successTime={}", result.getSuccessTime(), e);
            }
        }

        log.info("[WechatPayCallback] 回调解析完成，spMchid={}, subMchid={}, subAppid={}, outTradeNo={}, transactionId={}",
                maskMchId(cmd.getMchId()), maskMchId(cmd.getSubMchId()),
                maskAppId(cmd.getSubAppId()), cmd.getOutTradeNo(), cmd.getTransactionId());

        return cmd;
    }

    /**
     * 脱敏商户号。
     */
    private String maskMchId(String mchId) {
        if (mchId == null || mchId.length() <= 8) {
            return "***";
        }
        return mchId.substring(0, 4) + "***" + mchId.substring(mchId.length() - 4);
    }

    /**
     * 脱敏 AppID。
     */
    private String maskAppId(String appId) {
        if (appId == null || appId.length() <= 10) {
            return "***";
        }
        return appId.substring(0, 6) + "***" + appId.substring(appId.length() - 4);
    }
}
