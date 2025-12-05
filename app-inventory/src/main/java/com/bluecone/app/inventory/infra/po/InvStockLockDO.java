package com.bluecone.app.inventory.infra.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_inv_stock_lock")
public class InvStockLockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private Long orderId;

    private Long orderItemId;

    private Long lockQty;

    private Integer status;

    private LocalDateTime expireAt;

    private String requestId;

    private String remark;

    private String ext;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
