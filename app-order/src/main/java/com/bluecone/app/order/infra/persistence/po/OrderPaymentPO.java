package com.bluecone.app.order.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_order_payment")
public class OrderPaymentPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long orderId;

    private String payChannel;

    private String payStatus;

    private BigDecimal payAmount;

    private String currency;

    private String thirdTradeNo;

    private LocalDateTime payTime;

    private String extraJson;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
