package com.bluecone.app.infra.notify.timeline;

import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationTask;

/**
 * 时间线/限流/幂等编排入口（Timeline 层）。
 */
public interface NotificationTimeline {

    /**
     * 对单条任务做幂等与限流决策。
     *
     * @param task   通道任务
     * @param intent 原始意图
     * @return 决策
     */
    TimelineDecision decide(NotificationTask task, NotificationIntent intent);
}
