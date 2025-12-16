package com.bluecone.app.store.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.mybatis.Ulid128BinaryTypeHandler;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 门店只读快照表 DO，对应表 bc_store_read_model。
 * 
 * <p>注：此类依赖 app-id 的 Ulid128BinaryTypeHandler（MyBatis TypeHandler）。
 * TypeHandler 作为基础设施 SPI 实现，允许在数据访问层使用。
 */
@Data
@TableName("bc_store_read_model")
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

