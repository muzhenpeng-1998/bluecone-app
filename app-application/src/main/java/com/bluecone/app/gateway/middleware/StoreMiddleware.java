package com.bluecone.app.gateway.middleware;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import com.bluecone.app.store.api.StoreContextProvider;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gateway middleware: resolves storeId/store snapshot and injects into ApiContext.
 * Runs after tenant middleware so tenantId is already bound.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StoreMiddleware implements ApiMiddleware {

    public static final String ATTR_STORE_ORDER_SNAPSHOT = "STORE_ORDER_SNAPSHOT";
    public static final String ATTR_STORE_ID = "STORE_ID";
    public static final String ATTR_TENANT_ID = "TENANT_ID";

    private final StoreContextProvider storeContextProvider;

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        if (!requiresStoreContext(ctx)) {
            chain.next(ctx);
            return;
        }

        Long tenantId = parseLongSafe(ctx.getTenantId(), "tenantId");
        Long storeId = resolveStoreId(ctx);
        if (tenantId == null || storeId == null) {
            throw BusinessException.of(ErrorCode.PARAM_MISSING.getCode(), "storeId missing");
        }

        StoreOrderSnapshot snapshot = storeContextProvider.getOrderSnapshot(
                tenantId,
                storeId,
                LocalDateTime.now(),
                resolveChannelType(ctx));

        ctx.setStoreId(storeId);
        ctx.setStoreOrderSnapshot(snapshot);
        ctx.putAttribute(ATTR_STORE_ORDER_SNAPSHOT, snapshot);
        if (ctx.getRequest() != null) {
            ctx.getRequest().setAttribute(ATTR_TENANT_ID, tenantId);
            ctx.getRequest().setAttribute(ATTR_STORE_ID, storeId);
            ctx.getRequest().setAttribute(ATTR_STORE_ORDER_SNAPSHOT, snapshot);
        }

        log.debug("StoreMiddleware resolved storeId={}, tenantId={}, canAccept={}",
                storeId, tenantId, snapshot != null ? snapshot.getCanAcceptOrder() : null);

        chain.next(ctx);
    }

    private boolean requiresStoreContext(ApiContext ctx) {
        String path = ctx.getRequest() != null ? ctx.getRequest().getRequestURI() : null;
        if (path == null) {
            return false;
        }
        // Minimal heuristic: user-side order/product paths需要门店上下文
        return path.startsWith("/api/order/user/") || path.startsWith("/api/product/user/");
    }

    private Long resolveStoreId(ApiContext ctx) {
        if (ctx.getRequest() != null) {
            String header = ctx.getRequest().getHeader("X-Store-Id");
            Long byHeader = parseLongSafe(header, null);
            if (byHeader != null) {
                return byHeader;
            }
        }
        if (ctx.getQueryParams() != null && ctx.getQueryParams().containsKey("storeId")) {
            String[] values = ctx.getQueryParams().get("storeId");
            if (values != null && values.length > 0) {
                Long byQuery = parseLongSafe(values[0], null);
                if (byQuery != null) {
                    return byQuery;
                }
            }
        }
        if (ctx.getPathVariables() != null && ctx.getPathVariables().containsKey("storeId")) {
            return parseLongSafe(ctx.getPathVariables().get("storeId"), null);
        }
        return ctx.getStoreId();
    }

    private String resolveChannelType(ApiContext ctx) {
        if (ctx.getRequest() == null) {
            return null;
        }
        String header = ctx.getRequest().getHeader("X-Channel-Type");
        if (StringUtils.hasText(header)) {
            return header;
        }
        String[] query = ctx.getQueryParams() != null ? ctx.getQueryParams().get("channelType") : null;
        if (query != null && query.length > 0 && StringUtils.hasText(query[0])) {
            return query[0];
        }
        return null;
    }

    private Long parseLongSafe(String raw, String fieldName) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            if (fieldName != null) {
                throw BusinessException.of(ErrorCode.PARAM_INVALID.getCode(), fieldName + " invalid");
            }
            return null;
        }
    }
}

