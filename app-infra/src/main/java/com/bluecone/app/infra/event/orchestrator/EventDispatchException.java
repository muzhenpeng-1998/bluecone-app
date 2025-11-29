// File: app-infra/src/main/java/com/bluecone/app/infra/event/orchestrator/EventDispatchException.java
package com.bluecone.app.infra.event.orchestrator;

/**
 * 分发事件到 handler 时发生失败的运行时异常。
 *
 * <p>当某个 handler 异常时抛出，调用方可选择快速失败、重试或切换其他投递方式。
 * 未来可扩展为更丰富的错误模型（按 handler 分类、区分暂态/永久错误等）。</p>
 */
public class EventDispatchException extends RuntimeException {

    public EventDispatchException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
