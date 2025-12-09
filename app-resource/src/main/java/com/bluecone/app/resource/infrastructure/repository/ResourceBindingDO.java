package com.bluecone.app.resource.infrastructure.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资源绑定表 bc_res_binding 对应的 DO。
 */
@Data
@TableName("bc_res_binding")
public class ResourceBindingDO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("owner_type")
    private String ownerType;

    @TableField("owner_id")
    private Long ownerId;

    private String purpose;

    @TableField("resource_object_id")
    private Long resourceObjectId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_main")
    private Boolean isMain;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private Long createdBy;
}
