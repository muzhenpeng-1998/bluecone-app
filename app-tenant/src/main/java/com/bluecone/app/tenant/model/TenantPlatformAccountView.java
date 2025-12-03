package com.bluecone.app.tenant.model;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * 平台账号视图。
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
