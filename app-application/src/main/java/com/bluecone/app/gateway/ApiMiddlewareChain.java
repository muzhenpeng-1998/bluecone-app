package com.bluecone.app.gateway;

/**
 * Middleware dispatcher.
 */
public interface ApiMiddlewareChain {

    /**
     * Move to the next middleware in the chain.
     *
     * @param ctx gateway context
     * @throws Exception errors propagated up to error handling middleware
     */
    void next(ApiContext ctx) throws Exception;
}
