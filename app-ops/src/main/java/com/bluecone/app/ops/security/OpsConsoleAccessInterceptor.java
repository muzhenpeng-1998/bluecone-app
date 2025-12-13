package com.bluecone.app.ops.security;

import com.bluecone.app.ops.config.BlueconeOpsProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Simple access control interceptor for /ops endpoints.
 *
 * Only enforces read-only token checks and does not grant any application-level authorities.
 */
public class OpsConsoleAccessInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(OpsConsoleAccessInterceptor.class);

    private final BlueconeOpsProperties properties;

    public OpsConsoleAccessInterceptor(final BlueconeOpsProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String path = request.getRequestURI();

        if (!path.startsWith("/ops/") && !"/ops/console".equals(path) && !"/ops/api/summary".equals(path)) {
            return true;
        }

        if (!properties.isEnabled()) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return false;
        }

        // Misconfiguration: enabled=true but token is empty.
        if (!StringUtils.hasText(properties.getToken())) {
            log.error("[ops-console] enabled but token is empty, please configure bluecone.ops.console.token");
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return false;
        }

        if (isLocalBypass(request)) {
            return true;
        }

        String provided = resolveToken(request);
        if (!StringUtils.hasText(provided)) {
            log.warn("[ops-console] missing token for path={} remoteAddr={}", path, request.getRemoteAddr());
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return false;
        }

        if (!properties.getToken().equals(provided)) {
            log.warn("[ops-console] invalid token for path={} remoteAddr={} tokenMask={}",
                    path, request.getRemoteAddr(), maskToken(provided));
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return false;
        }

        return true;
    }

    private boolean isLocalBypass(HttpServletRequest request) {
        if (!properties.isAllowLocalhost()) {
            return false;
        }
        String addr = request.getRemoteAddr();
        return "127.0.0.1".equals(addr)
                || "0:0:0:0:0:0:0:1".equals(addr)
                || "::1".equals(addr)
                || "localhost".equalsIgnoreCase(addr);
    }

    private String resolveToken(HttpServletRequest request) {
        String headerToken = request.getHeader("X-Ops-Token");
        if (StringUtils.hasText(headerToken)) {
            return headerToken;
        }
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (properties.isAllowQueryToken()) {
            String queryToken = request.getParameter("token");
            if (StringUtils.hasText(queryToken)) {
                return queryToken;
            }
        }
        return null;
    }

    private String maskToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "<empty>";
        }
        String prefix = token.length() <= 4 ? token : token.substring(0, 4);
        return prefix + "****";
    }
}

