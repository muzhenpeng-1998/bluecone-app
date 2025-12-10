package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 租户主体详细资料
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Getter
@Setter
@Schema(name = "TenantProfile", description = "租户主体详细资料")
public class TenantProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    // 主键 ID，自增
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 租户 ID，关联 tenant.id
    @Schema(description = "租户ID")
    private Long tenantId;

    // 主体类型：1-企业，2-个体工商户，3-个人
    @Schema(description = "主体类型：1-企业，2-个体工商户，3-个人")
    private Byte tenantType;

    // 工商主体名称
    @Schema(description = "工商主体名称")
    private String businessName;

    // 营业执照注册号
    @Schema(description = "营业执照注册号")
    private String businessLicenseNo;

    // 营业执照图片地址
    @Schema(description = "营业执照图片")
    private String businessLicenseUrl;

    // 税号（加密存储）
    @Schema(description = "税号（加密）")
    private String taxNo;

    // 法人姓名
    @Schema(description = "法人姓名")
    private String legalPersonName;

    // 法人身份证号（加密存储）
    @Schema(description = "法人身份证号（加密）")
    private String legalPersonIdNo;

    // 主体注册地址
    @Schema(description = "主体注册地址")
    private String address;

    // 认证状态：0未认证，1提交认证，2审核中，3通过，4驳回
    @Schema(description = "认证状态：0未认证，1提交认证，2审核中，3通过，4驳回")
    private Byte verificationStatus;

    // 认证驳回原因
    @Schema(description = "驳回原因")
    private String verificationReason;

    // 逻辑删除标记：0-未删除，1-已删除
    @Schema(description = "逻辑删除：0-未删除，1-已删除")
    private Byte deleted;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

}
