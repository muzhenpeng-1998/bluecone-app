package com.bluecone.app.application.middleware;

import com.bluecone.app.config.InventoryContextProperties;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.context.ApiContextHolder;
import com.bluecone.app.inventory.domain.error.InventoryErrorCode;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;
import com.bluecone.app.inventory.runtime.application.InventoryPolicySnapshotProvider;
import com.bluecone.app.store.runtime.api.StoreContext;
import org.slf4j.MDC;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 从 ApiContext + StoreContext 解析库存策略快照并注入 ApiContext。
 */
public class InventoryContextResolver {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final InventoryPolicySnapshotProvider snapshotProvider;
    private final InventoryContextProperties props;

    public InventoryContextResolver(InventoryPolicySnapshotProvider snapshotProvider,
                                    InventoryContextProperties props) {
        this.snapshotProvider = snapshotProvider;
        this.props = props;
    }

    public InventoryPolicySnapshot resolve(ApiContext ctx) {
        if (!props.isEnabled() || ctx == null || ctx.getRequest() == null) {
            return null;
        }
        String path = ctx.getRequest().getRequestURI();
        if (!matchesPath(path)) {
            return null;
        }

        long tenantId = resolveTenantId(ctx);
        StoreContext storeContext = resolveStoreContext();

        Long storeNumericId = resolveStoreNumericId(storeContext);

        Optional<InventoryPolicySnapshot> snapshotOpt = snapshotProvider.getOrLoad(
                tenantId,
                storeContext.storeInternalId(),
                storeContext.storePublicId(),
                storeNumericId
        );
        InventoryPolicySnapshot snapshot = snapshotOpt.orElseThrow(
                () -> new BizException(InventoryErrorCode.INVENTORY_POLICY_NOT_FOUND));

        injectIntoApiContext(snapshot);
        writeMdc(snapshot);
        return snapshot;
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

    private long resolveTenantId(ApiContext ctx) {
        String tenantStr = ctx.getTenantId();
        if (!StringUtils.hasText(tenantStr)) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantStr);
        } catch (NumberFormatException ex) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }

    private StoreContext resolveStoreContext() {
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "门店上下文缺失，无法加载库存策略");
        }
        Object attr = apiCtx.getAttribute("STORE_CONTEXT");
        if (!(attr instanceof StoreContext storeContext)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "门店上下文缺失，无法加载库存策略");
        }
        return storeContext;
    }

    private Long resolveStoreNumericId(StoreContext storeContext) {
        if (storeContext.snapshot() == null || storeContext.snapshot().ext() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "门店上下文不包含数值型门店ID，无法加载库存策略");
        }
        Object storeIdObj = storeContext.snapshot().ext().get("storeId");
        if (storeIdObj instanceof Long l) {
            return l;
        }
        if (storeIdObj instanceof Number n) {
            return n.longValue();
        }
        throw new BizException(CommonErrorCode.BAD_REQUEST, "门店上下文不包含有效的数值型门店ID，无法加载库存策略");
    }

    private void injectIntoApiContext(InventoryPolicySnapshot snapshot) {
        ApiContext apiCtx = ApiContextHolder.get();
        if (apiCtx == null) {
            return;
        }
        apiCtx.setInventoryPolicySnapshot(snapshot);
        apiCtx.putAttribute("INVENTORY_POLICY_SNAPSHOT", snapshot);
    }

    private void writeMdc(InventoryPolicySnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (snapshot.deductMode() != null) {
            MDC.put("inventoryDeductMode", snapshot.deductMode());
        }
        MDC.put("inventoryPolicyVersion", String.valueOf(snapshot.configVersion()));
    }
}

