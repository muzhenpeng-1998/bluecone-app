package com.bluecone.app.notify.api.dto;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 用户偏好DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDTO {
    
    private Long id;
    private Long tenantId;
    private Long userId;
    private Map<NotificationChannel, Boolean> channelPreferences;
    private Boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private List<String> subscribedBizTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
