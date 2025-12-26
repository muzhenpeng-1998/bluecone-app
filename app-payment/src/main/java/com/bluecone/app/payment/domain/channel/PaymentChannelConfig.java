package com.bluecone.app.payment.domain.channel;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付渠道配置聚合根：某租户某门店的具体支付渠道参数。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentChannelConfig {

    private Long id;

    private Long tenantId;

    private Long storeId;

    private PaymentChannelType channelType;

    private String channelMode;

    private boolean enabled;

    private String notifyUrl;

    private WeChatChannelSecrets weChatSecrets;

    private String extJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean isWeChatChannel() {
        return channelType == PaymentChannelType.WECHAT_JSAPI || channelType == PaymentChannelType.WECHAT_NATIVE;
    }

    public void enable() {
        validateRequired();
        this.enabled = true;
    }

    public void disable() {
        validateRequired();
        this.enabled = false;
    }

    /**
     * 基础必填校验。
     */
    public void validateRequired() {
        if (tenantId == null || storeId == null || channelType == null) {
            throw new IllegalArgumentException("渠道配置缺少必填字段：tenantId/storeId/channelType");
        }
        // 当 channelType 为 WECHAT_* 时，channelMode 不能为空（为空则默认 SERVICE_PROVIDER）
        if (isWeChatChannel() && channelMode == null) {
            this.channelMode = "SERVICE_PROVIDER";
        }
    }
}
