package com.bluecone.app.infra.idresolve;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bluecone.app.id.core.Ulid128;
import lombok.Data;

/**
 * 公共 ID 映射表数据对象，对应表 bc_public_id_map。
 */
@Data
@TableName("bc_public_id_map")
public class PublicIdMapDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String resourceType;

    private String publicId;

    /**
     * 内部 ULID128，对应列 internal_id (BINARY(16))。
     */
    private Ulid128 internalId;

    /**
     * 状态：1 有效，0 无效（预留）。
     */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

