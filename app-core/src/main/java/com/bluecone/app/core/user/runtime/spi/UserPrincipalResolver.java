package com.bluecone.app.core.user.runtime.spi;

import com.bluecone.app.id.core.Ulid128;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * 用户主体解析 SPI：从请求中解析当前登录用户。
 */
public interface UserPrincipalResolver {

    Optional<UserPrincipal> resolve(HttpServletRequest req);

    record UserPrincipal(
            long tenantId,
            Ulid128 userId,
            String authType,
            String subject
    ) {
    }
}

