package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 用户通知偏好数据对象
 */
@Data
@TableName("bc_notify_user_preference")
public class UserPreferenceDO {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    private Long userId;
    private String channelPreferences;
    private Boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private String subscribedBizTypes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
