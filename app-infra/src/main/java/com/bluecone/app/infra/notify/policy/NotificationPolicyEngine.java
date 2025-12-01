package com.bluecone.app.infra.notify.policy;

import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationPlan;

/**
 * 通道路由与策略引擎（Policy 层）。
 */
public interface NotificationPolicyEngine {

    /**
     * 评估意图并生成任务计划。
     *
     * @param intent 内部意图
     * @return 任务计划，可为空计划
     */
    NotificationPlan evaluate(NotificationIntent intent);
}
