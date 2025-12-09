package com.bluecone.app.infra.webhook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Webhook 配置实体，对应表 bc_webhook_config。
 * 支持租户配置事件回调 URL + 签名密钥。
 */
@Data
@TableName("bc_webhook_config")
public class WebhookConfigDO {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 事件类型：ORDER_SUBMITTED / PAYMENT_SUCCESS / ORDER_ACCEPTED 等
     */
    private String eventType;

    /**
     * 回调 URL
     */
    private String targetUrl;

    /**
     * 签名密钥，用于 HMAC-SHA256 签名（可选）
     */
    private String secret;

    /**
     * 是否启用：1-启用，0-禁用
     */
    private Integer enabled;

    /**
     * 备注说明
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
