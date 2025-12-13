package com.bluecone.app.core.user.runtime;

import com.bluecone.app.core.user.runtime.spi.UserPrincipalResolver;
import com.bluecone.app.id.core.Ulid128;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 仅在 dev 环境启用的 Header 解析实现：X-User-Id。
 */
@Component
@Profile("dev")
public class DevHeaderUserPrincipalResolver implements UserPrincipalResolver {

    @Override
    public Optional<UserPrincipal> resolve(HttpServletRequest req) {
        if (req == null) {
            return Optional.empty();
        }
        String tenantHeader = req.getHeader("X-Tenant-Id");
        String userHeader = req.getHeader("X-User-Id");
        if (!StringUtils.hasText(tenantHeader) || !StringUtils.hasText(userHeader)) {
            return Optional.empty();
        }
        try {
            long tenantId = Long.parseLong(tenantHeader);
            long userId = Long.parseLong(userHeader);
            Ulid128 internalId = new Ulid128(userId, 0L);
            return Optional.of(new UserPrincipal(tenantId, internalId, "DEV_HEADER", userHeader));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}

