package com.bluecone.app.infra.configcenter.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Entity mapping for configuration properties stored in bc_config_property.
 */
@Data
@TableName("bc_config_property")
public class ConfigPropertyEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("config_key")
    private String configKey;

    @TableField("config_value")
    private String configValue;

    @TableField("value_type")
    private String valueType;

    @TableField("scope")
    private String scope;

    @TableField("env")
    private String env;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("enabled")
    private Boolean enabled;

    @TableField("version")
    private Integer version;

    @TableField("remark")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
