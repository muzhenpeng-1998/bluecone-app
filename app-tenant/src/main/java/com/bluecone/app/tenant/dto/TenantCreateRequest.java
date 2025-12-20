package com.bluecone.app.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TenantCreateRequest {

    // 租户名称，必填，最大 64 字符
    @NotBlank
    @Size(max = 64)
    private String tenantName;

    // 联系人姓名，必填，最大 32 字符
    @NotBlank
    @Size(max = 32)
    private String contactPerson;

    // 联系人手机号，必填，最大 32 字符
    @NotBlank
    @Size(max = 32)
    private String contactPhone;

    // 联系邮箱，可选，格式必须是合法邮箱
    @Email
    private String contactEmail;

    // 备注信息，可选，最大 128 字符
    @Size(max = 128)
    private String remark;

    // 租户主体类型：1-企业，2-个体工商户，3-个人，必填
    @NotNull
    private Byte tenantType;

    // 工商主体名称，必填
    @NotBlank
    private String businessName;

    // 营业执照注册号，可选
    private String businessLicenseNo;

    // 营业执照图片地址，可选
    private String businessLicenseUrl;

    // 法人姓名，可选
    private String legalPersonName;

    // 法人身份证号，可选
    private String legalPersonIdNo;

    // 主体注册地址，可选
    private String address;

    // 初始化订阅的套餐 ID，可选，空表示先挂在免费版
    private Long initialPlanId;

    // 初始化套餐到期时间，可选
    private LocalDateTime planExpireAt;

    /**
     * 操作人 ID，预留给审计使用，由安全层在网关/拦截器中填充。
     */
    private Long operatorId;
}
