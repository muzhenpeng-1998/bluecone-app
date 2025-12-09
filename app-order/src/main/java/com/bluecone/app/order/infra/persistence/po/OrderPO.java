package com.bluecone.app.order.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_order")
public class OrderPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long userId;

    private String sessionId;

    private Integer sessionVersion;

    private String orderNo;

    private String clientOrderNo;

    private String orderSource;

    private String bizType;

    private String channel;

    private BigDecimal totalAmount;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String currency;

    private String status;

    private String payStatus;

    private String orderRemark;

    private String extJson;

    @Version
    private Integer version;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    private Long acceptOperatorId;

    private LocalDateTime acceptedAt;

    // TODO: 确保 bc_order 表已包含 user_deleted / user_deleted_at 字段
    private Boolean userDeleted;

    private LocalDateTime userDeletedAt;
}
