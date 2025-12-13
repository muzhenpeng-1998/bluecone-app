package com.bluecone.app.gateway;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.bluecone.app.gateway.endpoint.ApiEndpoint;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import com.bluecone.app.inventory.runtime.api.InventoryPolicySnapshot;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-request gateway context carrying resolved metadata and response placeholder.
 */
@Getter
@Builder
public class ApiContext {

    private final String traceId;
    private final LocalDateTime requestTime;
    private final HttpServletRequest request;
    private final ApiContract contract;
    private final ApiEndpoint apiEndpoint;
    private final String apiVersion;
    private final Map<String, String> pathVariables;
    private final Map<String, String[]> queryParams;

    @Setter
    private String tenantId;

    @Setter
    private Long userId;

    @Setter
    private Long storeId;

    @Setter
    private StoreOrderSnapshot storeOrderSnapshot;

    @Setter
    private InventoryPolicySnapshot inventoryPolicySnapshot;

    @Setter
    private String clientType;

    @Setter
    private Object response;

    @Setter
    private Exception error;

    /**
     * Mutable attributes for downstream communication between middleware and handler.
     */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}
