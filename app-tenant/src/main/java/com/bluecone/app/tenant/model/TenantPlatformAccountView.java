package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 平台账号视图。
 * - 聚合平台类型、账号、授权状态/过期时间
 * - 用于详情展示和后续同步/解绑扩展
 */
@Value
@Builder
public class TenantPlatformAccountView {
    Long id;
    String platformType;
    String platformAccountId;
    String accountName;
    Byte status;
    LocalDateTime expireAt;
}
