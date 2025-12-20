package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.bluecone.app.id.core.Ulid128;
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

    // 主键 ID，自增
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 内部主键 ULID128，对应列 internal_id (BINARY(16))
    @Schema(description = "内部主键 ULID128")
    private Ulid128 internalId;

    // 对外租户 ID，对应列 public_id
    @Schema(description = "对外租户 ID")
    private String publicId;

    // 租户编码（业务唯一键）
    @Schema(description = "租户编码")
    private String tenantCode;

    // 租户名称
    @Schema(description = "租户名称")
    private String tenantName;

    // 租户状态：0-禁用，1-启用
    @Schema(description = "租户状态：0-禁用，1-启用")
    private Integer status;

    // 入驻状态：0-草稿（DRAFT），1-入驻完成（ACTIVE），2-关闭/终止（CLOSED）
    @Schema(description = "入驻状态：0-草稿（DRAFT），1-入驻完成（ACTIVE），2-关闭/终止（CLOSED）")
    private Integer onboardStatus;

    // 获客/招商渠道代码，例如 coffee-2025、douyin-ad-01
    @Schema(description = "获客/招商渠道代码，例如 coffee-2025、douyin-ad-01")
    private String sourceChannel;

    // 租户默认绑定的小程序 appid，用于该租户下大部分场景的默认小程序入口。
    @Schema(description = "租户默认绑定的小程序 appid，用于该租户下大部分场景的默认小程序入口。")
    private String defaultMiniappAppid;

    // 联系人姓名
    @Schema(description = "联系人")
    private String contactPerson;

    // 联系人电话
    @Schema(description = "联系电话")
    private String contactPhone;

    // 联系人邮箱
    @Schema(description = "联系邮箱")
    private String contactEmail;

    // 备注信息
    @Schema(description = "备注")
    private String remark;

    // 逻辑删除标记：0-未删除，1-已删除
    @Schema(description = "逻辑删除：0-未删除，1-已删除")
    private Integer deleted;

    // 创建时间
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    // 更新时间
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    public static final String ID = "id";

    public static final String TENANT_CODE = "tenant_code";

    public static final String TENANT_NAME = "tenant_name";

    public static final String STATUS = "status";

    public static final String ONBOARD_STATUS = "onboard_status";

    public static final String SOURCE_CHANNEL = "source_channel";

    public static final String DEFAULT_MINIAPP_APPID = "default_miniapp_appid";

    public static final String CONTACT_PERSON = "contact_person";

    public static final String CONTACT_PHONE = "contact_phone";

    public static final String CONTACT_EMAIL = "contact_email";

    public static final String REMARK = "remark";

    public static final String DELETED = "deleted";

    public static final String CREATED_AT = "created_at";

    public static final String UPDATED_AT = "updated_at";
}
