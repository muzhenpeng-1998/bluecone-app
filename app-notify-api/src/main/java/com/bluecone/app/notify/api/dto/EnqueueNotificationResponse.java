package com.bluecone.app.notify.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 入队通知响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnqueueNotificationResponse {
    
    /**
     * 创建的任务ID列表
     */
    private List<Long> taskIds;
    
    /**
     * 是否所有任务都创建成功
     */
    private boolean allCreated;
    
    /**
     * 失败的渠道及原因
     */
    private List<FailedChannel> failedChannels;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailedChannel {
        private String channel;
        private String reason;
    }
}
