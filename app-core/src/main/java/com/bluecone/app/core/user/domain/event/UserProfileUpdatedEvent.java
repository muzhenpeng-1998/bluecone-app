package com.bluecone.app.core.user.domain.event;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventMetadata;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * 用户资料更新事件（User Profile Updated Event），同步昵称/头像/手机号等改动。
 */
@Getter
public class UserProfileUpdatedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "USER_PROFILE_UPDATED";
    private static final String AGGREGATE_TYPE = "USER";

    private final Long userId;
    private final String nickname;
    private final String avatarUrl;
    private final String phone;
    private final String source;

    public UserProfileUpdatedEvent(Long userId,
                                   String nickname,
                                   String avatarUrl,
                                   String phone,
                                   String source) {
        super(EVENT_TYPE, buildMetadata(userId));
        this.userId = userId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.phone = phone;
        this.source = source;
    }

    private static EventMetadata buildMetadata(Long userId) {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("aggregateType", AGGREGATE_TYPE);
        if (userId != null) {
            attributes.put("aggregateId", String.valueOf(userId));
        }
        return EventMetadata.of(attributes);
    }
}
