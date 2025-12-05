package com.bluecone.app.core.user.domain.identity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 用户身份聚合根，对应表 bc_user_identity。
 * <p>包含平台注册账号的核心标识信息，后续可扩展多种注册渠道与风控能力。</p>
 */
@Data
public class UserIdentity {

    /** 平台用户 ID，主键 */
    private Long id;

    /** 微信 UnionId，全平台唯一 */
    private String unionId;

    /** 手机号 */
    private String phone;

    /** 国家区号 */
    private String countryCode;

    /** 邮箱 */
    private String email;

    /** 注册渠道 */
    private RegisterChannel registerChannel;

    /** 用户状态 */
    private UserStatus status;

    /** 首次注册关联的租户 ID */
    private Long firstTenantId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 基于微信小程序注册的工厂方法。
     */
    public static UserIdentity registerByWeChatMini(String unionId, String phone, String countryCode, Long firstTenantId, RegisterChannel channel) {
        throw new UnsupportedOperationException("TODO implement registerByWeChatMini");
    }

    /**
     * 冻结账号。
     */
    public void disable() {
        throw new UnsupportedOperationException("TODO implement disable");
    }

    /**
     * 解除冻结，恢复可用。
     */
    public void enable() {
        throw new UnsupportedOperationException("TODO implement enable");
    }
}
