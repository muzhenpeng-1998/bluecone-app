package com.bluecone.app.core.user.domain.identity;

/**
 * 注册渠道枚举，对应表 bc_user_identity.register_channel。
 */
public enum RegisterChannel {
    WECHAT_MINI,
    ALIPAY_MINI,
    PHONE,
    ADMIN_IMPORT
}
