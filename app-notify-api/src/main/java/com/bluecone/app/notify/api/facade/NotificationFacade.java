package com.bluecone.app.notify.api.facade;

import com.bluecone.app.notify.api.dto.EnqueueNotificationRequest;
import com.bluecone.app.notify.api.dto.EnqueueNotificationResponse;

/**
 * 通知服务门面
 * 业务模块通过此接口发起通知
 */
public interface NotificationFacade {
    
    /**
     * 入队通知任务
     * 根据业务事件和模板变量创建通知任务，由 dispatcher 异步发送
     * 
     * @param request 通知请求
     * @return 入队结果
     */
    EnqueueNotificationResponse enqueue(EnqueueNotificationRequest request);
    
    /**
     * 取消通知任务
     * 
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    boolean cancelTask(Long taskId);
    
    /**
     * 查询任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    String getTaskStatus(Long taskId);
}
