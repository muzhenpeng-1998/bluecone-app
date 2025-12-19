package com.bluecone.app.application.middleware;

import com.bluecone.app.config.StoreContextProperties;
import com.bluecone.app.core.idresolve.api.PublicIdResolver;
import com.bluecone.app.core.idresolve.api.ResolveKey;
import com.bluecone.app.core.idresolve.api.ResolveResult;
import com.bluecone.app.core.idresolve.api.PublicIdInvalidException;
import com.bluecone.app.core.idresolve.api.PublicIdNotFoundException;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.context.ApiContextHolder;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.store.domain.error.StoreErrorCode;
import com.bluecone.app.core.store.StoreContext;
import com.bluecone.app.core.store.StoreSnapshot;
import com.bluecone.app.store.runtime.application.StoreSnapshotProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 从请求中解析门店 publicId 并加载 StoreContext（含快照）。
 */
public class StoreContextResolver {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final PublicIdResolver publicIdResolver;
    private final StoreSnapshotProvider snapshotProvider;
    private final StoreContextProperties props;

    public StoreContextResolver(PublicIdResolver publicIdResolver,
                                StoreSnapshotProvider snapshotProvider,
                                StoreContextProperties props) {
        this.publicIdResolver = publicIdResolver;
        this.snapshotProvider = snapshotProvider;
        this.props = props;
    }

    public StoreContext resolve(HttpServletRequest request) {
        if (!props.isEnabled() || !matchesPath(request.getRequestURI())) {
            return null;
        }

        long tenantId = resolveTenantId(request);
        String storePublicId = resolveStorePublicId(request);

        if (!StringUtils.hasText(storePublicId)) {
            if (props.isRequireStoreId() && !isAllowMissingStoreIdPath(request.getRequestURI())) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "storeId 不能为空");
            }
            return null;
        }

        ResolveResult resolve = publicIdResolver.resolve(
                new ResolveKey(tenantId, ResourceType.STORE, storePublicId));
        if (!resolve.hit() || !resolve.exists()) {
            if (ResolveResult.REASON_INVALID_FORMAT.equals(resolve.reason())
                    || ResolveResult.REASON_PREFIX_MISMATCH.equals(resolve.reason())) {
                throw new PublicIdInvalidException("非法的门店标识");
            }
            throw new PublicIdNotFoundException("门店不存在");
        }

        Ulid128 internalId = resolve.internalId();
        Optional<StoreSnapshot> snapshotOpt = snapshotProvider.getOrLoad(tenantId, internalId, storePublicId);
        StoreSnapshot snapshot = snapshotOpt.orElseThrow(() ->
                new BusinessException(StoreErrorCode.STORE_NOT_FOUND, "门店不存在或已删除"));

        if (snapshot.status() == 0) {
            throw new BusinessException(StoreErrorCode.STORE_DISABLED, "门店已停用");
        }
        if (!snapshot.openForOrders()) {
            throw new BusinessException(StoreErrorCode.STORE_CLOSED_FOR_ORDERS, "门店当前不可接单");
        }

        StoreContext ctx = new StoreContext(tenantId, internalId, storePublicId, snapshot);
        injectIntoApiContext(ctx);
        writeMdc(ctx);
        return ctx;
    }

    private boolean matchesPath(String path) {
        for (String exclude : props.getExcludePaths()) {
            if (PATH_MATCHER.match(exclude, path)) {
                return false;
            }
        }
        for (String include : props.getIncludePaths()) {
            if (PATH_MATCHER.match(include, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAllowMissingStoreIdPath(String path) {
        for (String p : props.getAllowMissingStoreIdPaths()) {
            if (PATH_MATCHER.match(p, path)) {
                return true;
            }
        }
        return false;
    }

    private long resolveTenantId(HttpServletRequest request) {
        ApiContext apiCtx = ApiContextHolder.get();
        String tenantStr = null;
        if (apiCtx != null && apiCtx.getTenantId() != null) {
            tenantStr = apiCtx.getTenantId();
        } else if (TenantContext.getTenantId() != null) {
            tenantStr = TenantContext.getTenantId();
        } else {
            tenantStr = request.getHeader("X-Tenant-Id");
        }
        if (!StringUtils.hasText(tenantStr)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantStr);
        } catch (NumberFormatException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }

    private String resolveStorePublicId(HttpServletRequest request) {
        String header = request.getHeader("X-Store-Id");
        if (StringUtils.hasText(header)) {
            return header;
        }
        String query = request.getParameter("storeId");
        if (StringUtils.hasText(query)) {
            return query;
        }
        return null;
    }

    private void injectIntoApiContext(StoreContext storeContext) {
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx == null) {
            return;
        }
        apiCtx.setStoreId(null); // 保留 Long storeId 字段，后续可映射内部 Long ID 如有需要
        apiCtx.setStoreOrderSnapshot(null); // 下游可按需构造 StoreOrderSnapshot
        apiCtx.putAttribute("STORE_CONTEXT", storeContext);
    }

    private void writeMdc(StoreContext ctx) {
        MDC.put("tenantId", String.valueOf(ctx.tenantId()));
        MDC.put("storePublicId", ctx.storePublicId());
        String internalStr = ctx.storeInternalId() != null ? ctx.storeInternalId().toString() : null;
        if (internalStr != null && internalStr.length() > 10) {
            MDC.put("storeInternalId", internalStr.substring(0, 6) + "..." + internalStr.substring(internalStr.length() - 4));
        } else if (internalStr != null) {
            MDC.put("storeInternalId", internalStr);
        }
    }
}

