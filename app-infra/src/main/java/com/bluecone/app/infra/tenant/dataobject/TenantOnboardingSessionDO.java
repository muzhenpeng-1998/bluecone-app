package com.bluecone.app.infra.tenant.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 租户入驻引导会话表映射，表名：bc_tenant_onboarding_session。
 * 用于串联扫码-H5-填表-注册完整流程。
 */
@Data
@TableName("bc_tenant_onboarding_session")
public class TenantOnboardingSessionDO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 入驻会话 token，前端 H5 存于 cookie/localStorage
     */
    private String sessionToken;

    /**
     * 渠道标识，例如 coffee-2025、douyin-ad-01
     */
    private String channelCode;

    /**
     * 平台用户 ID，对应 bc_user_identity.id
     */
    private Long userId;

    /**
     * 关联租户 ID，创建租户草稿后回填
     */
    private Long tenantId;

    /**
     * 关联门店 ID，创建首家门店草稿后回填
     */
    private Long storeId;

    /**
     * 联系人手机号，便于后续招商跟进
     */
    private String contactPhone;

    /**
     * 会话状态：0-初始化，1-填写中，2-已提交，3-关闭/过期
     */
    private Integer status;

    /**
     * 扩展字段 JSON，记录原始表单快照、AB 实验标记等
     */
    private String extJson;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
