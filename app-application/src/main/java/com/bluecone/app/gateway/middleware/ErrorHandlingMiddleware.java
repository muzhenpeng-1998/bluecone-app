package com.bluecone.app.gateway.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;

/**
 * Converts exceptions into unified error responses.
 */
@Component
public class ErrorHandlingMiddleware implements ApiMiddleware {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingMiddleware.class);

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) {
        try {
            chain.next(ctx);
        } catch (BusinessException ex) {
            ctx.setError(ex);
            ctx.setResponse(ApiErrorResponse.of(ex.getCode(), ex.getMessage(), path(ctx)));
        } catch (Exception ex) {
            log.error("Gateway internal error", ex);
            ctx.setError(ex);
            ctx.setResponse(ApiErrorResponse.of(
                    ErrorCode.INTERNAL_ERROR.getCode(),
                    ErrorCode.INTERNAL_ERROR.getMessage(),
                    path(ctx)));
        }
    }

    private String path(ApiContext ctx) {
        return ctx.getRequest() != null ? ctx.getRequest().getRequestURI() : "";
    }
}
