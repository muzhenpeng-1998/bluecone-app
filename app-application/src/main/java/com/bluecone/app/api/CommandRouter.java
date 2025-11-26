package com.bluecone.app.api;

import com.bluecone.app.core.api.ApiHandler;
import com.bluecone.app.core.api.ApiRequest;
import com.bluecone.app.core.api.VersionExtractor;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.infra.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 命令路由器 - API 版本分发核心组件
 * 职责：
 * 1. 解析请求版本号
 * 2. 根据 CommandKey + Version 定位 Handler Bean
 * 3. 构造 ApiRequest 并调用 Handler
 * 4. 支持版本回退策略（可选）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommandRouter {

    private final ApplicationContext applicationContext;
    private final VersionExtractor versionExtractor;

    /**
     * 路由请求到对应版本的 Handler
     *
     * @param commandKey 命令标识（如 "Order.Detail"）
     * @param request HTTP 请求对象
     * @return Handler 处理结果
     * @throws Exception 业务异常或系统异常
     */
    public Object route(String commandKey, HttpServletRequest request) throws Exception {
        // 1. 解析版本号
        int version = versionExtractor.extract(request);

        // 2. 查找对应版本的 Handler
        ApiHandler handler = findHandler(commandKey, version);

        // 3. 构造 ApiRequest
        ApiRequest apiRequest = buildApiRequest(request, version);

        // 4. 调用 Handler 处理
        log.debug("Routing command [{}] to version [{}]", commandKey, version);
        return handler.handle(apiRequest);
    }

    /**
     * 查找 Handler Bean，支持版本回退
     *
     * 策略：请求 v3 不存在时，尝试 v2 → v1
     */
    private ApiHandler findHandler(String commandKey, int requestedVersion) {
        int currentVersion = requestedVersion;

        // 从请求版本开始向下回退
        while (currentVersion >= 1) {
            String beanName = buildBeanName(commandKey, currentVersion);

            if (applicationContext.containsBean(beanName)) {
                ApiHandler handler = applicationContext.getBean(beanName, ApiHandler.class);

                if (currentVersion < requestedVersion) {
                    log.warn("Handler for [{}] v{} not found, fallback to v{}",
                        commandKey, requestedVersion, currentVersion);
                }

                return handler;
            }

            currentVersion--;
        }

        // 所有版本都不存在
        throw new BusinessException(
            ErrorCode.UNSUPPORTED_VERSION.getCode(),
            String.format("No handler found for command [%s] version [%d]", commandKey, requestedVersion)
        );
    }

    /**
     * 构造 Handler Bean 名称
     * 约定：{CommandKey}.v{Version}
     */
    private String buildBeanName(String commandKey, int version) {
        return commandKey + ".v" + version;
    }

    /**
     * 构造 ApiRequest 对象
     */
    private ApiRequest buildApiRequest(HttpServletRequest request, int version) {
        String tenantIdStr = TenantContext.getTenantId();
        Long tenantId = null;
        if (tenantIdStr != null) {
            try {
                tenantId = Long.valueOf(tenantIdStr);
            } catch (NumberFormatException e) {
                // 忽略非数字的 tenantId
            }
        }

        return ApiRequest.builder()
            .rawRequest(request)
            .method(request.getMethod())
            .path(request.getRequestURI())
            .queryParams(request.getParameterMap())
            .headers(extractHeaders(request))
            .body(extractBody(request))
            .version(version)
            .tenantId(tenantId)
            .userId(getUserId())
            .build();
    }

    /**
     * 提取所有 Header
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        return headers;
    }

    /**
     * 提取请求体（简化实现）
     */
    private String extractBody(HttpServletRequest request) {
        try {
            return request.getReader().lines()
                .reduce("", (acc, line) -> acc + line);
        } catch (Exception e) {
            log.warn("Failed to extract request body", e);
            return null;
        }
    }

    /**
     * 从 MDC 获取用户 ID
     */
    private Long getUserId() {
        String userId = MDC.get("userId");
        return userId != null ? Long.parseLong(userId) : null;
    }
}
