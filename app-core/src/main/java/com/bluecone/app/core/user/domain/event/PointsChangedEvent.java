package com.bluecone.app.core.user.domain.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 积分变动事件，便于对账与通知。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsChangedEvent {

    private Long tenantId;

    private Long memberId;

    private int changePoints;

    private int balanceAfter;

    private String bizType;

    private String bizId;

    private LocalDateTime occurredAt;
}
