package com.bluecone.app.core.idempotency.api;

import java.util.function.Supplier;

/**
 * 幂等执行模板，对外提供统一的业务幂等门面。
 */
public interface IdempotencyTemplate {

    /**
     * 在幂等上下文中执行指定业务逻辑。
     *
     * <p>模板负责处理幂等记录创建、请求冲突校验、并发控制以及结果重放。</p>
     *
     * @param request    幂等请求参数
     * @param resultType 期望返回结果类型
     * @param supplier   实际业务执行逻辑（仅在获得执行权时被调用）
     * @param <T>        返回值类型
     * @return 幂等执行结果，包含是否重放/处理中以及业务返回值
     */
    <T> IdempotentResult<T> execute(IdempotencyRequest request, Class<T> resultType, Supplier<T> supplier);
}

