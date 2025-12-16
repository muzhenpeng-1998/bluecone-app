package com.bluecone.app.store.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.internal.governance.AllowIdInfraAccess;
import com.bluecone.app.id.internal.mybatis.Ulid128BinaryTypeHandler;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 门店只读快照表 DO，对应表 bc_store_read_model。
 */
@Data
@TableName("bc_store_read_model")
@AllowIdInfraAccess
public class BcStoreReadModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "store_internal_id", type = IdType.INPUT)
    @TableField(typeHandler = Ulid128BinaryTypeHandler.class)
    private Ulid128 storeInternalId;

    private String publicId;

    private Long storeNo;

    private Long tenantId;

    private String storeName;

    private LocalDateTime updatedAt;
}

