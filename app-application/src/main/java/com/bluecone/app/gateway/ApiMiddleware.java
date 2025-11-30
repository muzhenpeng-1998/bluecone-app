package com.bluecone.app.gateway;

/**
 * Functional unit for cross-cutting concerns executed before handler invocation.
 */
public interface ApiMiddleware {

    /**
     * Apply middleware logic and delegate to the next element in the chain.
     *
     * @param ctx   gateway context
     * @param chain middleware chain dispatcher
     * @throws Exception propagate to be handled by error middleware
     */
    void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception;
}
