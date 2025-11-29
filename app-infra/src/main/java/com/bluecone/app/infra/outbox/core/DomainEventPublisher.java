// File: app-infra/src/main/java/com/bluecone/app/infra/outbox/core/DomainEventPublisher.java
package com.bluecone.app.infra.outbox.core;

import com.bluecone.app.core.event.DomainEvent;

/**
 * 兼容保留的发布接口，已上移到 app-core。
 *
 * <p>建议业务依赖 {@link com.bluecone.app.core.event.DomainEventPublisher}，此接口向下兼容。</p>
 */
@Deprecated
public interface DomainEventPublisher extends com.bluecone.app.core.event.DomainEventPublisher {
}
