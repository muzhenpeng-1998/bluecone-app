package com.bluecone.app.core.user.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import lombok.Getter;

/**
 * 会员等级变更事件。
 */
@Getter
public class MemberLevelChangedEvent extends DomainEvent {

    private final Long tenantId;

    private final Long memberId;

    private final Long oldLevelId;

    private final Long newLevelId;

    public MemberLevelChangedEvent(Long tenantId,
                                   Long memberId,
                                   Long oldLevelId,
                                   Long newLevelId) {
        super("member.level.changed", EventMetadata.empty());
        this.tenantId = tenantId;
        this.memberId = memberId;
        this.oldLevelId = oldLevelId;
        this.newLevelId = newLevelId;
    }
}
