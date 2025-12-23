package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户外部身份绑定表映射，表名：bc_user_external_identity。
 * 
 * 用于绑定微信 openId、支付宝 userId 等外部身份标识。
 * 当 unionId 为空时，使用 (provider, appId, openId) 作为兜底唯一标识。
 */
@Data
@TableName("bc_user_external_identity")
public class UserExternalIdentityDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 身份提供方：WECHAT_MINI、WECHAT_H5、ALIPAY 等
     */
    private String provider;

    /**
     * 外部应用ID（微信小程序 appId、支付宝 appId 等）
     */
    private String appId;

    /**
     * 外部用户ID（微信 openId、支付宝 userId 等）
     */
    private String openId;

    /**
     * 外部 UnionId（可为空，微信 unionId、支付宝 userId 等）
     */
    private String unionId;

    /**
     * 平台用户ID（关联 bc_user_identity.id）
     */
    private Long userId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

