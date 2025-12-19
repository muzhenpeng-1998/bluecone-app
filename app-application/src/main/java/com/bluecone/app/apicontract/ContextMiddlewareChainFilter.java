package com.bluecone.app.apicontract;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.config.ApiContractProperties;
import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.idresolve.api.PublicIdInvalidException;
import com.bluecone.app.core.idresolve.api.PublicIdNotFoundException;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.context.ApiContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Orchestrates context middlewares based on the current route contract.
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
public class ContextMiddlewareChainFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApiContractProperties properties;
    private final List<ContextMiddleware> middlewares;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        if (isHardExcluded(path)) {
            return true;
        }
        // Only filter when a contract has been matched by ApiContractFilter.
        return request.getAttribute(ApiContractFilter.ATTR_API_SIDE) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ApiSide side = (ApiSide) request.getAttribute(ApiContractFilter.ATTR_API_SIDE);
        @SuppressWarnings("unchecked")
        EnumSet<ContextType> required = (EnumSet<ContextType>) request.getAttribute(ApiContractFilter.ATTR_REQUIRED_CONTEXTS);
        @SuppressWarnings("unchecked")
        EnumSet<ContextType> optional = (EnumSet<ContextType>) request.getAttribute(ApiContractFilter.ATTR_OPTIONAL_CONTEXTS);

        ApiContext existing = ApiContextHolder.get();
        boolean createdHere = false;
        ApiContext ctx = existing;
        if (ctx == null) {
            ctx = ApiContext.builder()
                    .traceId(null)
                    .requestTime(LocalDateTime.now())
                    .request(request)
                    .contract(null)
                    .apiEndpoint(null)
                    .apiVersion(null)
                    .pathVariables(null)
                    .queryParams(request.getParameterMap())
                    .build();
            createdHere = true;
            ApiContextHolder.set(ctx);
        }
        ctx.setApiSide(side);
        ctx.setRequiredContexts(required != null ? required : EnumSet.noneOf(ContextType.class));
        ctx.setOptionalContexts(optional != null ? optional : EnumSet.noneOf(ContextType.class));

        List<ContextMiddleware> ordered = new ArrayList<>(middlewares);
        ordered.sort(Comparator.comparingInt(ContextMiddleware::order));

        try {
            if ((required == null || required.isEmpty()) && (optional == null || optional.isEmpty())) {
                // 没有任何上下文需求，仅写入 ApiSide 等元信息后直接放行。
                filterChain.doFilter(request, response);
                return;
            }

            for (ContextMiddleware middleware : ordered) {
                ContextType type = middleware.type();
                boolean requiredType = required != null && required.contains(type);
                boolean optionalType = optional != null && optional.contains(type);
                if (!requiredType && !optionalType) {
                    continue;
                }
                if (!middleware.supports(side)) {
                    continue;
                }

                try {
                    middleware.apply(request, response, filterChain, ctx);
                } catch (Exception ex) {
                    if (requiredType) {
                        handleContextFailure(response, ex);
                        return;
                    }
                    log.debug("Optional context middleware {} failed for type={} side={}, ignoring",
                            middleware.getClass().getSimpleName(), type, side, ex);
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            if (createdHere) {
                ApiContextHolder.clear();
            }
        }
    }

    private boolean isHardExcluded(String path) {
        if (path == null) {
            return false;
        }
        return PATH_MATCHER.match("/ops/**", path)
                || PATH_MATCHER.match("/actuator/**", path);
    }

    private void handleContextFailure(HttpServletResponse response, Exception ex) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        int status = determineStatus(ex);
        String code = determineErrorCode(ex);
        String message = ex.getMessage() != null ? ex.getMessage() : "Context resolution failed";

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.fail(code, message);
        String json = "{\"code\":\"" + body.getCode() + "\"," +
                "\"message\":\"" + escapeJson(body.getMessage()) + "\"," +
                "\"data\":null," +
                "\"traceId\":\"" + body.getTraceId() + "\"," +
                "\"timestamp\":\"" + body.getTimestamp() + "\"}";
        response.getWriter().write(json);
    }

    private int determineStatus(Exception ex) {
        if (ex instanceof PublicIdInvalidException) {
            return HttpStatus.BAD_REQUEST.value();
        }
        if (ex instanceof PublicIdNotFoundException) {
            return HttpStatus.NOT_FOUND.value();
        }
        if (ex instanceof BusinessException biz) {
            return mapBizErrorCodeToStatus(biz.getCode());
        }
        return HttpStatus.BAD_REQUEST.value();
    }

    private String determineErrorCode(Exception ex) {
        if (ex instanceof BusinessException biz) {
            return biz.getCode();
        }
        if (ex instanceof PublicIdInvalidException) {
            return CommonErrorCode.BAD_REQUEST.getCode();
        }
        if (ex instanceof PublicIdNotFoundException) {
            return ErrorCode.NOT_FOUND.getCode();
        }
        return CommonErrorCode.BAD_REQUEST.getCode();
    }

    private int mapBizErrorCodeToStatus(String code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST.value();
        }
        if (code.contains("-404-")) {
            return HttpStatus.NOT_FOUND.value();
        }
        if (code.contains("-410-")) {
            return HttpStatus.GONE.value();
        }
        if (code.contains("-403-")) {
            return HttpStatus.FORBIDDEN.value();
        }
        if (code.contains("-409-")) {
            return HttpStatus.CONFLICT.value();
        }
        if (CommonErrorCode.UNAUTHORIZED.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED.value();
        }
        if (CommonErrorCode.FORBIDDEN.getCode().equals(code)) {
            return HttpStatus.FORBIDDEN.value();
        }
        if (CommonErrorCode.CONFLICT.getCode().equals(code)) {
            return HttpStatus.CONFLICT.value();
        }
        if (CommonErrorCode.SYSTEM_ERROR.getCode().equals(code)) {
            return HttpStatus.INTERNAL_SERVER_ERROR.value();
        }
        if (CommonErrorCode.BAD_REQUEST.getCode().equals(code)) {
            return HttpStatus.BAD_REQUEST.value();
        }
        return HttpStatus.BAD_REQUEST.value();
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
