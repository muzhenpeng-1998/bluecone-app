package com.bluecone.app.gateway.middleware;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import com.bluecone.app.gateway.response.ResponseEnvelope;

/**
 * Wraps handler outputs into a consistent envelope.
 */
@Component
public class ResponseWrapperMiddleware implements ApiMiddleware {

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        chain.next(ctx);
        Object response = ctx.getResponse();
        if (response instanceof ResponseEnvelope<?> || response instanceof ApiErrorResponse) {
            return;
        }
        ResponseEnvelope<Object> envelope = ResponseEnvelope.<Object>builder()
                .data(response)
                .traceId(MDC.get("traceId"))
                .build();
        ctx.setResponse(envelope);
    }
}
