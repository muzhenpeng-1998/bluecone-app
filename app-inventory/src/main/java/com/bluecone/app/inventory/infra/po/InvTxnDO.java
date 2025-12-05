package com.bluecone.app.inventory.infra.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_inv_txn")
public class InvTxnDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private Long itemId;

    private Long locationId;

    private String txnType;

    private String txnDirection;

    private Long qty;

    private Long beforeTotal;

    private Long afterTotal;

    private Long beforeLocked;

    private Long afterLocked;

    private String bizRefType;

    private Long bizRefId;

    private Long lockId;

    private String requestId;

    private String extra;

    private LocalDateTime createdAt;
}
