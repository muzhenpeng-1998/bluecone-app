package com.bluecone.app.api.onboarding.dto;

/**
 * 一键开通微信小程序响应。
 */
public class WechatRegisterResponse {

    /**
     * 注册任务 ID。
     */
    private Long taskId;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }
}

