package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 小料组实体，对应表 {@code bc_addon_group}。
 * <p>
 * 在租户层维护可复用的小料组（加料组），如“奶茶配料”“咖啡加料”，可配置类型、状态与排序。
 */
@Data
@TableName("bc_addon_group")
public class AddonGroupEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 小料组ID，自增主键。
     * 对应表字段：id。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID。
     * 对应表字段：tenant_id。
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 小料组名称，如“奶茶配料”。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 类型：1计价小料，2不计价小料。
     * 对应表字段：type。
     */
    @TableField("type")
    private Integer type;

    /**
     * 状态：1启用，0禁用。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值。
     * 对应表字段：sort_order。
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 备注。
     * 对应表字段：remark。
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间。
     * 对应表字段：created_at。
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间，自动更新。
     * 对应表字段：updated_at。
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
