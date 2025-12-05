package com.bluecone.app.order.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_order_item")
public class OrderItemPO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long orderId;

    private Long productId;

    private Long skuId;

    private String productName;

    private String skuName;

    private String productCode;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    private BigDecimal payableAmount;

    private String attrsJson;

    private String remark;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;
}
