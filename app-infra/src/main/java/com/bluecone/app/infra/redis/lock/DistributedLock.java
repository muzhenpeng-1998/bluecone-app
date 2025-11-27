package com.bluecone.app.infra.redis.lock;

/**
 * 分布式锁抽象接口，便于未来扩展不同实现（Redis、Zookeeper、数据库等）。
 */
public interface DistributedLock {

    /**
     * 尝试在指定时间内获取分布式锁。
     *
     * @param bizKey      业务键（未拼环境、租户前缀）
     * @param ownerId     持有者标识，用于防止误删他人锁
     * @param waitTimeMs  等待获取锁的最长时间，毫秒
     * @param leaseTimeMs 锁租约时间，毫秒，到期自动释放
     * @return 成功获取返回 true，否则 false
     */
    boolean tryLock(String bizKey, String ownerId, long waitTimeMs, long leaseTimeMs);

    /**
     * 释放分布式锁，仅在当前持有者与 ownerId 匹配时才会删除。
     *
     * @param bizKey  业务键（未拼环境、租户前缀）
     * @param ownerId 持有者标识
     */
    void unlock(String bizKey, String ownerId);
}
