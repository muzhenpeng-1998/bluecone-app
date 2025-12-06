package com.bluecone.app.payment.infra.persistence.channel;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 支付渠道配置 DO，对应表 bc_payment_channel_config。
 */
@Data
@TableName("bc_payment_channel_config")
public class PaymentChannelConfigDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private String channelCode;

    private Integer enabled;

    private String notifyUrl;

    private String mchId;

    private String appId;

    private String subMchId;

    private String encApiV3Key;

    private String encSerialNo;

    private String extJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
