package com.bluecone.app.gateway.middleware;

import com.bluecone.app.application.middleware.InventoryContextResolver;
import com.bluecone.app.config.InventoryContextProperties;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

/**
 * 库存上下文中间件：基于 StoreContext + InventoryPolicySnapshotProvider。
 */
@Slf4j
public class InventoryMiddleware implements ApiMiddleware {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final InventoryContextResolver inventoryContextResolver;
    private final InventoryContextProperties props;

    public InventoryMiddleware(InventoryContextResolver inventoryContextResolver,
                               InventoryContextProperties props) {
        this.inventoryContextResolver = inventoryContextResolver;
        this.props = props;
    }

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        HttpServletRequest request = ctx.getRequest();
        if (!props.isEnabled() || request == null || !matchesPath(request.getRequestURI())) {
            chain.next(ctx);
            return;
        }

        inventoryContextResolver.resolve(ctx);
        chain.next(ctx);
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
}

