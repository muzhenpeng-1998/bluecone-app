package com.bluecone.app.product.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 门店菜单快照实体，对应表 {@code bc_store_menu_snapshot}。
 * <p>
 * 用于存储门店在特定渠道与场景下的菜单 JSON 快照，支撑小程序/前端高并发拉取菜单，包含乐观锁版本和生成时间。
 */
@Data
@TableName("bc_store_menu_snapshot")
public class StoreMenuSnapshotEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键，自增。
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
     * 门店ID。
     * 对应表字段：store_id。
     */
    @TableField("store_id")
    private Long storeId;

    /**
     * 渠道：DINE_IN/DELIVERY/PICKUP 等，默认 ALL。
     * 对应表字段：channel。
     */
    @TableField("channel")
    private String channel;

    /**
     * 场景：DEFAULT/BREAKFAST/LUNCH/DINNER/NIGHT 等，用于分时菜单。
     * 对应表字段：order_scene。
     */
    @TableField("order_scene")
    private String orderScene;

    /**
     * 逻辑版本号，用于乐观锁控制快照刷新。
     * 对应表字段：version。
     */
    @TableField("version")
    private Long version;

    /**
     * 菜单快照 JSON，直接供前端/网关消费。
     * 对应表字段：menu_json。
     */
    @TableField("menu_json")
    private String menuJson;

    /**
     * 快照生成时间。
     * 对应表字段：generated_at。
     */
    @TableField("generated_at")
    private LocalDateTime generatedAt;

    /**
     * 状态：1有效，0无效。
     * 对应表字段：status。
     */
    @TableField("status")
    private Integer status;

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
