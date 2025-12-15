package com.bluecone.app.gateway.middleware;

import com.bluecone.app.apicontract.ContextMiddleware;
import com.bluecone.app.application.middleware.InventoryContextResolver;
import com.bluecone.app.config.InventoryContextProperties;
import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.AntPathMatcher;

/**
 * 库存上下文中间件：基于 StoreContext + InventoryPolicySnapshotProvider。
 */
@Slf4j
public class InventoryMiddleware implements ApiMiddleware, ContextMiddleware {

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

    @Override
    public ContextType type() {
        return ContextType.INVENTORY;
    }

    @Override
    public int order() {
        return 400;
    }

    @Override
    public boolean supports(ApiSide side) {
        // Inventory context currently only makes sense where store context is present,
        // typically USER / MERCHANT paths.
        return side == null || side == ApiSide.USER || side == ApiSide.MERCHANT;
    }

    @Override
    public void apply(HttpServletRequest request,
                      HttpServletResponse response,
                      FilterChain chain,
                      ApiContext ctx) throws Exception {
        if (!props.isEnabled() || request == null) {
            return;
        }
        inventoryContextResolver.resolve(ctx);
    }
}
