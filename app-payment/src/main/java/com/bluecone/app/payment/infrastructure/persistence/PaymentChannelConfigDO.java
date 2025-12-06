package com.bluecone.app.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 支付渠道配置数据对象，对应表 {@code bc_payment_channel_config}。
 */
@Data
@TableName("bc_payment_channel_config")
public class PaymentChannelConfigDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private String channelType;

    private String channelMode;

    private String appId;

    private String mchId;

    private String subMchId;

    private String encryptPayload;

    private Integer status;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
