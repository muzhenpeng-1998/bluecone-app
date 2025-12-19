package com.bluecone.app.order.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 订单动作幂等日志表 PO。
 */
@Data
@TableName("bc_order_action_log")
public class OrderActionLogPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long orderId;

    private String actionType;

    private String actionKey;

    private Long operatorId;

    private String operatorName;

    private String status;

    private String resultJson;

    private String errorCode;

    private String errorMsg;

    private String extJson;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
