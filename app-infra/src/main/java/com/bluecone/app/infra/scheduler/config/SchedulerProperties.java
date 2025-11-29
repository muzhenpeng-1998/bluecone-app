package com.bluecone.app.infra.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调度中心可配置参数。
 */
@ConfigurationProperties(prefix = "bluecone.scheduler")
public class SchedulerProperties {

    /**
     * 全局开关。
     */
    private boolean enabled = true;

    /**
     * 心跳周期（秒）。
     */
    private int loopIntervalSeconds = 3;

    /**
     * Worker 线程数。
     */
    private int workerThreads = 4;

    /**
     * 队列 key。
     */
    private String queueKey = "bluecone:scheduler:queue:exec";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLoopIntervalSeconds() {
        return loopIntervalSeconds;
    }

    public void setLoopIntervalSeconds(int loopIntervalSeconds) {
        this.loopIntervalSeconds = loopIntervalSeconds;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public String getQueueKey() {
        return queueKey;
    }

    public void setQueueKey(String queueKey) {
        this.queueKey = queueKey;
    }
}
