package com.bluecone.app.core.user.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 会员等级变更事件（Member Level Changed Event）。
 */
@Getter
public class MemberLevelChangedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "MEMBER_LEVEL_CHANGED";
    private static final String AGGREGATE_TYPE = "MEMBER";

    private final Long tenantId;
    private final Long storeId;
    private final Long userId;
    private final Long memberId;
    private final Long oldLevelId;
    private final Long newLevelId;
    private final String oldLevelCode;
    private final String newLevelCode;
    private final Long operatorId;

    public MemberLevelChangedEvent(Long tenantId,
                                   Long storeId,
                                   Long userId,
                                   Long memberId,
                                   Long oldLevelId,
                                   Long newLevelId,
                                   String oldLevelCode,
                                   String newLevelCode,
                                   Long operatorId) {
        super(EVENT_TYPE, buildMetadata(memberId, tenantId));
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.userId = userId;
        this.memberId = memberId;
        this.oldLevelId = oldLevelId;
        this.newLevelId = newLevelId;
        this.oldLevelCode = oldLevelCode;
        this.newLevelCode = newLevelCode;
        this.operatorId = operatorId;
    }

    private static EventMetadata buildMetadata(Long memberId, Long tenantId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (memberId != null) {
            attributes.put("aggregateId", String.valueOf(memberId));
        }
        if (tenantId != null) {
            attributes.put("tenantId", String.valueOf(tenantId));
        }
        return EventMetadata.of(attributes);
    }
}
