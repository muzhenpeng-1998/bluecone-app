package com.bluecone.app.tenant.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 第三方平台账号绑定信息（微信/支付宝/抖音等）。
 */
@Data
@TableName("tenant_platform_account")
@Schema(name = "TenantPlatformAccount", description = "第三方平台账号绑定信息")
public class TenantPlatformAccount implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "平台类型：wechat、alipay、douyin 等")
    private String platformType;

    @Schema(description = "平台内账号ID或appId")
    private String platformAccountId;

    @Schema(description = "账号昵称或名称")
    private String accountName;

    @Schema(description = "授权凭证（可存密文）")
    private String credential;

    @Schema(description = "授权状态：0未绑定，1已绑定，2已过期")
    private Byte status;

    @Schema(description = "授权到期时间")
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
