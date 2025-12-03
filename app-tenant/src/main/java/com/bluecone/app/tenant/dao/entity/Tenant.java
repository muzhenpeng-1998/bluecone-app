package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 租户表
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-01
 */
@Getter
@Setter
@Schema(name = "Tenant", description = "租户表")
public class Tenant implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "租户状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "联系邮箱")
    private String contactEmail;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "逻辑删除：0-未删除，1-已删除")
    private Integer deleted;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public static final String ID = "id";

    public static final String TENANT_CODE = "tenant_code";

    public static final String TENANT_NAME = "tenant_name";

    public static final String STATUS = "status";

    public static final String CONTACT_PERSON = "contact_person";

    public static final String CONTACT_PHONE = "contact_phone";

    public static final String CONTACT_EMAIL = "contact_email";

    public static final String REMARK = "remark";

    public static final String DELETED = "deleted";

    public static final String CREATED_AT = "created_at";

    public static final String UPDATED_AT = "updated_at";
}
