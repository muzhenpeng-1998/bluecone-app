package com.bluecone.app.order.controller.support;

import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Helper to fetch tenant/store/user info and store snapshot from current request attributes.
 */
public final class RequestContextHelper {

    private static final String ATTR_TENANT_ID = "TENANT_ID";
    private static final String ATTR_STORE_ID = "STORE_ID";
    private static final String ATTR_STORE_ORDER_SNAPSHOT = "STORE_ORDER_SNAPSHOT";

    private RequestContextHelper() {
    }

    public static Long currentTenantId() {
        String tenantFromCtx = TenantContext.getTenantId();
        if (StringUtils.hasText(tenantFromCtx)) {
            return parseLong(tenantFromCtx);
        }
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        Object attr = req.getAttribute(ATTR_TENANT_ID);
        if (attr instanceof Long) {
            return (Long) attr;
        }
        if (attr instanceof String) {
            return parseLong((String) attr);
        }
        String header = req.getHeader("X-Tenant-Id");
        if (StringUtils.hasText(header)) {
            return parseLong(header);
        }
        String param = req.getParameter("tenantId");
        if (StringUtils.hasText(param)) {
            return parseLong(param);
        }
        return null;
    }

    public static Long currentStoreId() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        Object attr = req.getAttribute(ATTR_STORE_ID);
        if (attr instanceof Long) {
            return (Long) attr;
        }
        if (attr instanceof String) {
            return parseLong((String) attr);
        }
        String header = req.getHeader("X-Store-Id");
        if (StringUtils.hasText(header)) {
            return parseLong(header);
        }
        String param = req.getParameter("storeId");
        if (StringUtils.hasText(param)) {
            return parseLong(param);
        }
        return null;
    }

    public static Long currentUserId() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        Object attr = req.getAttribute("X-User-Id");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        if (attr instanceof String) {
            return parseLong((String) attr);
        }
        String header = req.getHeader("X-User-Id");
        if (StringUtils.hasText(header)) {
            return parseLong(header);
        }
        String param = req.getParameter("userId");
        if (StringUtils.hasText(param)) {
            return parseLong(param);
        }
        return null;
    }

    public static StoreOrderSnapshot currentStoreSnapshot() {
        HttpServletRequest req = currentRequest();
        if (req == null) {
            return null;
        }
        Object attr = req.getAttribute(ATTR_STORE_ORDER_SNAPSHOT);
        if (attr instanceof StoreOrderSnapshot) {
            return (StoreOrderSnapshot) attr;
        }
        return null;
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private static Long parseLong(String raw) {
        try {
            return Long.valueOf(raw);
        } catch (Exception ex) {
            return null;
        }
    }
}

