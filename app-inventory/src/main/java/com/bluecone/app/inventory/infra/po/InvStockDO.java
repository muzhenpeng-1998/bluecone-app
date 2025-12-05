package com.bluecone.app.inventory.infra.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_inv_stock")
public class InvStockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private Long totalQty;

    private Long lockedQty;

    private Long availableQty;

    private Long safetyStock;

    private Long version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
