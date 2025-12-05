package com.bluecone.app.core.user.domain.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 入会事件，对应会员创建流程。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantMemberEnrolledEvent {

    private Long tenantId;

    private Long memberId;

    private Long userId;

    private String joinChannel;

    private LocalDateTime occurredAt;
}
