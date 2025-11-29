package com.bluecone.app.infra.scheduler.core;

/**
 * 任务处理器插件接口。
 *
 * <p>实现类使用 {@link com.bluecone.app.infra.scheduler.annotation.BlueconeJob}
 * 注解声明元数据。</p>
 */
public interface JobHandler {

    /**
     * 执行任务逻辑，调用方负责超时与上下文隔离。
     *
     * @param context 任务上下文
     * @throws Exception 业务异常将被记录为失败
     */
    void handle(JobContext context) throws Exception;
}
