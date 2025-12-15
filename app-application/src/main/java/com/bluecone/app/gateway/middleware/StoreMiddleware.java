package com.bluecone.app.gateway.middleware;

import com.bluecone.app.apicontract.ContextMiddleware;
import com.bluecone.app.application.middleware.StoreContextResolver;
import com.bluecone.app.config.StoreContextProperties;
import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * 新版门店上下文中间件：基于 publicId + StoreSnapshotProvider。
 */
@Slf4j
@Component
public class StoreMiddleware implements ApiMiddleware, ContextMiddleware {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ObjectProvider<StoreContextResolver> storeContextResolverProvider;
    private final StoreContextProperties props;

    public StoreMiddleware(ObjectProvider<StoreContextResolver> storeContextResolverProvider,
                           StoreContextProperties props) {
        this.storeContextResolverProvider = storeContextResolverProvider;
        this.props = props;
    }

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        HttpServletRequest request = ctx.getRequest();
        StoreContextResolver resolver = storeContextResolverProvider.getIfAvailable();
        if (!props.isEnabled() || resolver == null || request == null || !matchesPath(request.getRequestURI())) {
            chain.next(ctx);
            return;
        }

        resolver.resolve(request);
        chain.next(ctx);
    }

    @Override
    public ContextType type() {
        return ContextType.STORE;
    }

    @Override
    public int order() {
        // Run before user/product/inventory by default.
        return 100;
    }

    @Override
    public boolean supports(ApiSide side) {
        // Store context is meaningful for USER and MERCHANT sides.
        return side == null || side == ApiSide.USER || side == ApiSide.MERCHANT;
    }

    @Override
    public void apply(HttpServletRequest request,
                      HttpServletResponse response,
                      FilterChain chain,
                      ApiContext ctx) throws Exception {
        StoreContextResolver resolver = storeContextResolverProvider.getIfAvailable();
        if (!props.isEnabled() || resolver == null || request == null) {
            return;
        }
        resolver.resolve(request);
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
