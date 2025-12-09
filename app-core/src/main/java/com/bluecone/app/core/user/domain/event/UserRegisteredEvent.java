package com.bluecone.app.core.user.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 用户注册事件（User Registered Event）。
 */
@Getter
public class UserRegisteredEvent extends DomainEvent {

    public static final String EVENT_TYPE = "USER_REGISTERED";
    private static final String AGGREGATE_TYPE = "USER";

    private final Long userId;
    private final Long firstTenantId;
    private final String unionId;
    private final String phone;
    private final String registerChannel;

    public UserRegisteredEvent(Long userId,
                               Long firstTenantId,
                               String unionId,
                               String phone,
                               String registerChannel) {
        super(EVENT_TYPE, buildMetadata(userId, firstTenantId));
        this.userId = userId;
        this.firstTenantId = firstTenantId;
        this.unionId = unionId;
        this.phone = phone;
        this.registerChannel = registerChannel;
    }

    private static EventMetadata buildMetadata(Long userId, Long tenantId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (userId != null) {
            attributes.put("aggregateId", String.valueOf(userId));
        }
        if (tenantId != null) {
            attributes.put("tenantId", String.valueOf(tenantId));
        }
        return EventMetadata.of(attributes);
    }
}
