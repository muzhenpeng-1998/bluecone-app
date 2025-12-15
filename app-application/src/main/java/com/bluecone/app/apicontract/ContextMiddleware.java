package com.bluecone.app.apicontract;

import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import com.bluecone.app.gateway.ApiContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Pluggable context middleware executed based on route contracts.
 */
public interface ContextMiddleware {

    /**
     * Type of context this middleware is responsible for.
     */
    ContextType type();

    /**
     * Execution order; lower values are executed first.
     */
    int order();

    /**
     * Whether this middleware supports the given API side.
     *
     * @param side side derived from the current route contract
     * @return {@code true} if supported
     */
    default boolean supports(ApiSide side) {
        return true;
    }

    /**
     * Apply context resolution logic.
     *
     * <p>Implementations are expected to resolve and inject context into {@link ApiContext}
     * or other holders. Implementations should usually <strong>not</strong> call
     * {@link FilterChain#doFilter(HttpServletRequest, HttpServletResponse)} directly; the
     * outer filter will advance the servlet filter chain once all middlewares complete.</p>
     */
    void apply(HttpServletRequest request,
               HttpServletResponse response,
               FilterChain chain,
               ApiContext ctx) throws Exception;
}

