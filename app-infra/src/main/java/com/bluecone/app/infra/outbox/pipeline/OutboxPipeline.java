// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/pipeline/OutboxPipeline.java
package com.bluecone.app.infra.outbox.pipeline;

/**
 * Outbox 发布链中的可插拔步骤。
 */
public interface OutboxPipeline {

    /**
     * 执行管道步骤。
     *
     * @param context 发布上下文
     */
    void execute(OutboxPublishContext context);
}
