package com.bluecone.app.gateway;

/**
 * Core handler contract for gateway APIs.
 * <p>
 * Handlers only focus on domain logic. Cross-cutting concerns (auth, tenant,
 * rate-limit, logging, etc.) are handled by middleware.
 */
public interface ApiHandler<Req, Resp> {

    /**
     * Execute business logic for an API endpoint.
     *
     * @param ctx     gateway request context
     * @param request typed request body or null for GET-like calls
     * @return business response object
     * @throws Exception propagated to error middleware for uniform handling
     */
    Resp handle(ApiContext ctx, Req request) throws Exception;
}
