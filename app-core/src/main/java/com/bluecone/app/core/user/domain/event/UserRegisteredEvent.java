package com.bluecone.app.core.user.domain.event;

import java.time.LocalDateTime;

import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户注册完成事件，用于跨领域通知。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private Long userId;

    private Long firstTenantId;

    private RegisterChannel registerChannel;

    private LocalDateTime occurredAt;
}
