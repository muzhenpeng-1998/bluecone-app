package com.bluecone.app.core.create.api;

/**
 * 幂等创建模板，对外提供统一的 create 接口执行范式。
 */
public interface IdempotentCreateTemplate {

    /**
     * 在幂等上下文中执行一次创建操作。
     *
     * @param request 创建请求参数
     * @param work    具体创建逻辑，接受 internalId 与 publicId
     * @param <T>     返回类型
     * @return 包含重放标记、处理中标记、publicId、internalId 与业务返回值的结果
     */
    <T> IdempotentCreateResult<T> create(CreateRequest request, CreateWork<T> work);

    /**
     * 在幂等上下文中执行一次创建操作，并在同一事务中收集需要发布的领域事件。
     *
     * <p>事件会通过 TransactionalEventPublisher 在事务提交前写入 Outbox，提交成功后由调度器异步投递。</p>
     *
     * @param request 创建请求参数
     * @param work    具体创建逻辑，返回结果与待发布事件
     * @param <T>     返回类型
     * @return 包含重放标记、处理中标记、publicId、internalId 与业务返回值的结果
     */
    <T> IdempotentCreateResult<T> create(CreateRequest request, CreateWorkWithEvents<T> work);
}
