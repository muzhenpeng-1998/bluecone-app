package com.bluecone.app.migration.id;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ID 回填 Job 配置。
 */
@ConfigurationProperties(prefix = "bluecone.migration.id-backfill")
public class IdBackfillProperties {

    /**
     * 是否启用回填 Job。
     */
    private boolean enabled = false;

    /**
     * 目标表列表，例如：bc_store,bc_order。
     */
    private List<String> targets = new ArrayList<>();

    /**
     * 批大小，建议 500~2000。
     */
    private int batchSize = 1000;

    /**
     * 起始游标（基于旧主键 id），默认从 0 开始。
     */
    private long startId = 0L;

    /**
     * 是否为 dry-run 模式：只打印日志，不执行 UPDATE。
     */
    private boolean dryRun = true;

    /**
     * 每个批次之间的休眠毫秒数，用于避免压垮数据库。
     */
    private long sleepMillis = 0L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getStartId() {
        return startId;
    }

    public void setStartId(long startId) {
        this.startId = startId;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public long getSleepMillis() {
        return sleepMillis;
    }

    public void setSleepMillis(long sleepMillis) {
        this.sleepMillis = sleepMillis;
    }
}

