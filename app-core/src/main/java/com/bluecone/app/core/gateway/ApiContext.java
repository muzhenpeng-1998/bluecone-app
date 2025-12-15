package com.bluecone.app.core.gateway;

import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Per-request gateway context carrying resolved metadata and response placeholder.
 * 
 * <p>This is a minimal version in app-core to avoid circular dependencies.
 * The full version in app-application extends this with additional fields.</p>
 */
@Getter
@Builder
public class ApiContext {

    private final String traceId;
    private final LocalDateTime requestTime;

    /**
     * Logical side of the API handling this request.
     */
    @Setter
    private ApiSide apiSide;

    /**
     * Context types that must be resolved successfully for this request.
     */
    @Setter
    @Builder.Default
    private EnumSet<ContextType> requiredContexts = EnumSet.noneOf(ContextType.class);

    /**
     * Context types that can be resolved opportunistically; failures should not block the request.
     */
    @Setter
    @Builder.Default
    private EnumSet<ContextType> optionalContexts = EnumSet.noneOf(ContextType.class);

    @Setter
    private String tenantId;

    @Setter
    private Long userId;

    @Setter
    private Long storeId;

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

