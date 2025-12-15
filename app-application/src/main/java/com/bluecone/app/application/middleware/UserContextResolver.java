package com.bluecone.app.application.middleware;

import com.bluecone.app.config.UserContextProperties;
import com.bluecone.app.core.cacheepoch.api.CacheEpochProvider;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.error.UserErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.core.user.runtime.api.UserSnapshot;
import com.bluecone.app.core.user.runtime.spi.UserPrincipalResolver;
import com.bluecone.app.core.user.runtime.spi.UserSnapshotRepository;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.SnapshotLoadKey;
import com.bluecone.app.core.contextkit.SnapshotProvider;
import com.bluecone.app.core.contextkit.SnapshotSerde;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.context.ApiContextHolder;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * 从请求中解析用户主体并加载 UserSnapshot，然后注入 ApiContext。
 */
public class UserContextResolver {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final UserPrincipalResolver principalResolver;
    private final UserSnapshotRepository snapshotRepository;
    private final SnapshotProvider<UserSnapshot> snapshotProvider;
    private final SnapshotSerde<UserSnapshot> serde;
    private final ContextCache cache;
    private final VersionChecker versionChecker;
    private final ContextKitProperties kitProperties;
    private final UserContextProperties props;
    private final CacheEpochProvider epochProvider;

    public UserContextResolver(UserPrincipalResolver principalResolver,
                               UserSnapshotRepository snapshotRepository,
                               ContextCache cache,
                               VersionChecker versionChecker,
                               ContextKitProperties kitProperties,
                               UserContextProperties props,
                               ObjectMapper objectMapper) {
        this(principalResolver, snapshotRepository, cache, versionChecker, kitProperties, props, objectMapper, null);
    }

    public UserContextResolver(UserPrincipalResolver principalResolver,
                               UserSnapshotRepository snapshotRepository,
                               ContextCache cache,
                               VersionChecker versionChecker,
                               ContextKitProperties kitProperties,
                               UserContextProperties props,
                               ObjectMapper objectMapper,
                               CacheEpochProvider epochProvider) {
        this.principalResolver = principalResolver;
        this.snapshotRepository = snapshotRepository;
        this.cache = cache;
        this.versionChecker = versionChecker;
        this.kitProperties = kitProperties;
        this.props = props;
        this.snapshotProvider = new SnapshotProvider<>();
        this.serde = new UserSnapshotSerde(objectMapper);
        this.epochProvider = epochProvider;
    }

    public UserSnapshot resolve(ApiContext ctx) {
        if (!props.isEnabled() || ctx == null || ctx.getRequest() == null) {
            return null;
        }
        String path = ctx.getRequest().getRequestURI();
        if (!matchesInclude(path)) {
            return null;
        }
        if (matchesExclude(path)) {
            return null;
        }

        Optional<UserPrincipalResolver.UserPrincipal> principalOpt = principalResolver.resolve(ctx.getRequest());
        if (principalOpt.isEmpty()) {
            if (isAllowAnonymous(path)) {
                injectAnonymous();
                return null;
            }
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "未登录或登录已失效");
        }

        UserPrincipalResolver.UserPrincipal principal = principalOpt.get();
        long tenantId = principal.tenantId();

        SnapshotLoadKey loadKey = new SnapshotLoadKey(tenantId, CacheNamespaces.USER_SNAPSHOT, principal.userId());
        UserSnapshot snapshot = snapshotProvider.getOrLoad(
                loadKey,
                snapshotRepository,
                cache,
                versionChecker,
                serde,
                kitProperties,
                epochProvider
        );
        if (snapshot == null) {
            throw new BizException(UserErrorCode.USER_NOT_FOUND, "用户不存在或已删除");
        }
        if (snapshot.status() == 0) {
            throw new BizException(UserErrorCode.USER_FROZEN, "用户已冻结");
        }
        if (snapshot.status() == -1) {
            throw new BizException(UserErrorCode.USER_DELETED, "用户已注销");
        }

        injectIntoApiContext(snapshot);
        writeMdc(principal, snapshot);
        return snapshot;
    }

    private boolean matchesInclude(String path) {
        for (String include : props.getIncludePaths()) {
            if (PATH_MATCHER.match(include, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesExclude(String path) {
        for (String exclude : props.getExcludePaths()) {
            if (PATH_MATCHER.match(exclude, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowAnonymous(String path) {
        for (String p : props.getAllowAnonymousPaths()) {
            if (PATH_MATCHER.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    private void injectAnonymous() {
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx == null) {
            return;
        }
        apiCtx.setUserId(null);
        apiCtx.putAttribute("USER_SNAPSHOT", null);
    }

    private void injectIntoApiContext(UserSnapshot snapshot) {
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx == null) {
            return;
        }
        // ApiContext.userId 继续使用 Long（兼容现有业务），从快照 ext 中回填数值型 ID（如有）
        Long numericUserId = null;
        if (snapshot.ext() != null) {
            Object val = snapshot.ext().get("numericUserId");
            if (val instanceof Long l) {
                numericUserId = l;
            } else if (val instanceof Number n) {
                numericUserId = n.longValue();
            }
        }
        apiCtx.setUserId(numericUserId);
        apiCtx.putAttribute("USER_SNAPSHOT", snapshot);
    }

    private void writeMdc(UserPrincipalResolver.UserPrincipal principal, UserSnapshot snapshot) {
        if (principal != null && principal.userId() != null) {
            String raw = principal.userId().toString();
            String masked = raw.length() > 6
                    ? raw.substring(0, 3) + "..." + raw.substring(raw.length() - 3)
                    : raw;
            MDC.put("userId", masked);
        }
        if (principal != null && StringUtils.hasText(principal.authType())) {
            MDC.put("authType", principal.authType());
        }
    }

    private static class UserSnapshotSerde implements SnapshotSerde<UserSnapshot> {

        private final ObjectMapper objectMapper;

        private UserSnapshotSerde(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object toCacheValue(UserSnapshot value) {
            return value;
        }

        @Override
        public UserSnapshot fromCacheValue(Object cached) {
            if (cached == null) {
                return null;
            }
            if (cached instanceof UserSnapshot snapshot) {
                return snapshot;
            }
            return objectMapper.convertValue(cached, UserSnapshot.class);
        }
    }
}
