package com.bluecone.app.notify.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户通知偏好领域模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {
    
    private Long id;
    private Long tenantId;
    private Long userId;
    private String channelPreferencesJson;
    private Boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private String subscribedBizTypesJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 判断当前是否在免打扰时间
     */
    public boolean isInQuietHours() {
        if (!Boolean.TRUE.equals(quietHoursEnabled) || quietHoursStart == null || quietHoursEnd == null) {
            return false;
        }
        
        LocalTime now = LocalTime.now();
        
        // 处理跨天情况（如 22:00 - 08:00）
        if (quietHoursStart.isAfter(quietHoursEnd)) {
            return now.isAfter(quietHoursStart) || now.isBefore(quietHoursEnd);
        } else {
            return now.isAfter(quietHoursStart) && now.isBefore(quietHoursEnd);
        }
    }
}
