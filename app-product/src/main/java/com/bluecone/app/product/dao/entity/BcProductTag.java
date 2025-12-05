package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 商品标签实体，对应表 {@code bc_product_tag}。
 * <p>
 * 维护运营标签（如“新品”“热销”“低卡”），包含名称与样式配置，供商品关联使用，用于前台展示与运营筛选。
 */
@Data
@TableName("bc_product_tag")
public class BcProductTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 标签ID，自增主键。
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
     * 标签名称。
     * 对应表字段：name。
     */
    @TableField("name")
    private String name;

    /**
     * 样式配置，JSON 格式，包含颜色、图标、展示文案等。
     * 对应表字段：style。
     */
    @TableField("style")
    private String style;

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
