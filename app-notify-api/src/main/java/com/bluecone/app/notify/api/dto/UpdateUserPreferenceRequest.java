package com.bluecone.app.notify.api.dto;

import com.bluecone.app.notify.api.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * 更新用户偏好请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserPreferenceRequest {
    
    /**
     * 渠道偏好（渠道 -> 是否启用）
     */
    private Map<NotificationChannel, Boolean> channelPreferences;
    
    /**
     * 是否启用免打扰
     */
    private Boolean quietHoursEnabled;
    
    /**
     * 免打扰开始时间
     */
    private LocalTime quietHoursStart;
    
    /**
     * 免打扰结束时间
     */
    private LocalTime quietHoursEnd;
    
    /**
     * 订阅的业务类型（NULL=全部订阅）
     */
    private List<String> subscribedBizTypes;
}
