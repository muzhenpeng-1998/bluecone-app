package com.bluecone.app.gateway;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.endpoint.ApiEndpoint;
import com.bluecone.app.gateway.middleware.AuthMiddleware;
import com.bluecone.app.gateway.middleware.ErrorHandlingMiddleware;
import com.bluecone.app.gateway.middleware.IdempotentMiddleware;
import com.bluecone.app.gateway.middleware.LoggingMiddleware;
import com.bluecone.app.gateway.middleware.RateLimitMiddleware;
import com.bluecone.app.gateway.middleware.ResponseWrapperMiddleware;
import com.bluecone.app.gateway.middleware.SignatureMiddleware;
import com.bluecone.app.gateway.middleware.TenantMiddleware;
import com.bluecone.app.gateway.middleware.VersionMiddleware;
import com.bluecone.app.gateway.response.ResponseEnvelope;
import com.bluecone.app.gateway.routing.ApiRoute;
import com.bluecone.app.gateway.routing.ApiRouteRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Central gateway dispatcher: route resolution + middleware orchestration.
 */
@Component
@RequiredArgsConstructor
public class ApiGateway {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiRouteRegistry routeRegistry;
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;

    private final LoggingMiddleware loggingMiddleware;
    private final VersionMiddleware versionMiddleware;
    private final AuthMiddleware authMiddleware;
    private final TenantMiddleware tenantMiddleware;
    private final RateLimitMiddleware rateLimitMiddleware;
    private final IdempotentMiddleware idempotentMiddleware;
    private final SignatureMiddleware signatureMiddleware;
    private final ErrorHandlingMiddleware errorHandlingMiddleware;
    private final ResponseWrapperMiddleware responseWrapperMiddleware;

    public Object handle(HttpServletRequest request) {
        HttpMethod method = resolveHttpMethod(request.getMethod());
        ApiRoute route = routeRegistry.findRoute(method, request.getRequestURI())
                .orElseThrow(() -> BusinessException.of(ErrorCode.NOT_FOUND.getCode(), "API route not found"));
        ApiContract contract = route.getContract();

        Map<String, String> pathVariables = PATH_MATCHER.extractUriTemplateVariables(
                contract.getPath(), request.getRequestURI());
        ApiContext ctx = ApiContext.builder()
                .traceId(resolveTraceId(request))
                .requestTime(LocalDateTime.now())
                .request(request)
                .contract(contract)
                .apiEndpoint(resolveEndpoint(contract.getCode()))
                .apiVersion(resolveVersion(request))
                .pathVariables(pathVariables)
                .queryParams(request.getParameterMap())
                .build();

        List<ApiMiddleware> chain = buildChain(contract);
        ApiMiddlewareChain dispatcher = new DefaultApiMiddlewareChain(chain);

        try {
            dispatcher.next(ctx);
            return Optional.ofNullable(ctx.getResponse())
                    .orElse(ResponseEnvelope.builder()
                            .traceId(ctx.getTraceId())
                            .build());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private HttpMethod resolveHttpMethod(String rawMethod) {
        try {
            return HttpMethod.valueOf(rawMethod);
        } catch (IllegalArgumentException ex) {
            throw BusinessException.of(ErrorCode.INVALID_PARAM.getCode(), "Unsupported HTTP method");
        }
    }

    private List<ApiMiddleware> buildChain(ApiContract contract) {
        List<ApiMiddleware> chain = new ArrayList<>();
        chain.add(loggingMiddleware);
        chain.add(versionMiddleware);
        chain.add(errorHandlingMiddleware);
        if (contract.isAuthRequired()) {
            chain.add(authMiddleware);
        }
        if (contract.isTenantRequired()) {
            chain.add(tenantMiddleware);
        }
        if (contract.isRateLimitEnabled()) {
            chain.add(rateLimitMiddleware);
        }
        if (contract.isIdempotent()) {
            chain.add(idempotentMiddleware);
        }
        if (contract.isSignatureRequired()) {
            chain.add(signatureMiddleware);
        }
        chain.add(new HandlerInvokerMiddleware(contract));
        chain.add(responseWrapperMiddleware);
        return chain;
    }

    private String resolveTraceId(HttpServletRequest request) {
        String incoming = request.getHeader("X-Trace-Id");
        if (StringUtils.hasText(incoming)) {
            return incoming;
        }
        return UUID.randomUUID().toString();
    }

    private ApiEndpoint resolveEndpoint(String code) {
        for (ApiEndpoint endpoint : ApiEndpoint.values()) {
            if (endpoint.getCode().equalsIgnoreCase(code)) {
                return endpoint;
            }
        }
        return null;
    }

    private String resolveVersion(HttpServletRequest request) {
        String header = request.getHeader("X-Api-Version");
        if (StringUtils.hasText(header)) {
            return header.toLowerCase().startsWith("v") ? header.toLowerCase() : "v" + header.toLowerCase();
        }
        String query = request.getParameter("version");
        if (StringUtils.hasText(query)) {
            return query.toLowerCase().startsWith("v") ? query.toLowerCase() : "v" + query.toLowerCase();
        }
        String path = request.getRequestURI();
        if (path.matches(".*/api/v\\d+/.*")) {
            int start = path.indexOf("/api/v") + "/api/".length();
            String versionPart = path.substring(start, path.indexOf("/", start));
            return versionPart;
        }
        return null;
    }

    /**
     * Default chain implementation iterating through middleware list.
     */
    private static class DefaultApiMiddlewareChain implements ApiMiddlewareChain {

        private final List<ApiMiddleware> middlewares;
        private int index = 0;

        private DefaultApiMiddlewareChain(List<ApiMiddleware> middlewares) {
            this.middlewares = middlewares;
        }

        @Override
        public void next(ApiContext ctx) throws Exception {
            if (index >= middlewares.size()) {
                return;
            }
            ApiMiddleware current = middlewares.get(index++);
            current.apply(ctx, this);
        }
    }

    /**
     * Invokes the ApiHandler associated with the current contract.
     */
    private class HandlerInvokerMiddleware implements ApiMiddleware {

        private final ApiContract contract;

        HandlerInvokerMiddleware(ApiContract contract) {
            this.contract = contract;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
            ApiHandler<Object, Object> handler = (ApiHandler<Object, Object>) applicationContext.getBean(contract.getHandlerClass());
            Object req = deserializeRequestIfNeeded(ctx);
            Object resp = handler.handle(ctx, req);
            ctx.setResponse(resp);
            chain.next(ctx);
        }

        private Object deserializeRequestIfNeeded(ApiContext ctx) throws IOException {
            if (contract.getRequestType() == null) {
                return null;
            }
            if (ctx.getRequest() == null) {
                return null;
            }
            if (!supportsRequestBody(contract.getHttpMethod())) {
                return null;
            }
            return objectMapper.readValue(ctx.getRequest().getInputStream(), contract.getRequestType());
        }

        private boolean supportsRequestBody(HttpMethod method) {
            if (method == null) {
                return false;
            }
            return HttpMethod.POST.equals(method)
                    || HttpMethod.PUT.equals(method)
                    || HttpMethod.PATCH.equals(method)
                    || HttpMethod.DELETE.equals(method);
        }
    }
}
