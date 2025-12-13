package com.bluecone.app.core.user.runtime;

import com.bluecone.app.core.user.runtime.spi.UserPrincipalResolver;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import com.bluecone.app.security.core.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 默认的 UserPrincipalResolver 实现：从 JWT 解析用户主体。
 */
@Component
public class UserPrincipalResolverImpl implements UserPrincipalResolver {

    private final TokenProvider tokenProvider;

    public UserPrincipalResolverImpl(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Optional<UserPrincipal> resolve(HttpServletRequest req) {
        if (req == null) {
            return Optional.empty();
        }
        String header = req.getHeader(SecurityConstants.AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        TokenUserContext ctx = tokenProvider.parseAccessToken(token);
        if (ctx.getUserId() == null || ctx.getTenantId() == null) {
            return Optional.empty();
        }
        Ulid128 internalId = new Ulid128(ctx.getUserId(), 0L);
        return Optional.of(new UserPrincipal(ctx.getTenantId(), internalId, "JWT", String.valueOf(ctx.getUserId())));
    }
}

