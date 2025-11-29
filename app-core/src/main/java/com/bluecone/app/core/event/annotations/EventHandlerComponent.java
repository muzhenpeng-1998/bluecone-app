// File: app-core/src/main/java/com/bluecone/app/core/event/annotations/EventHandlerComponent.java
package com.bluecone.app.core.event.annotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件处理器的语义化标记。
 *
 * <p>封装 {@link Component}，在代码与 IDE 中一眼识别 handler 角色，同时保持标准 Spring Bean 行为。</p>
 */
@Component
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EventHandlerComponent {
}
