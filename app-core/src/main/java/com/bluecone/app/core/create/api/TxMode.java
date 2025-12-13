package com.bluecone.app.core.create.api;

/**
 * 幂等创建事务模式。
 */
public enum TxMode {

    /**
     * 使用新事务提交创建操作与幂等成功标记（推荐）。
     */
    REQUIRES_NEW,

    /**
     * 加入当前事务（不推荐，除非明确控制外层事务边界）。
     */
    REQUIRED,

    /**
     * 仅感知事务，不开启新事务。
     *
     * <p>无事务时直接落库，风险最大，慎用。</p>
     */
    AWARE_ONLY
}

