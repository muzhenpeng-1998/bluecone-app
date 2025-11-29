// File: app-infra/src/main/java/com/bluecone/app/infra/event/router/EventHandlerTypeResolver.java
package com.bluecone.app.infra.event.router;

import com.bluecone.app.core.event.DomainEvent;
import com.bluecone.app.core.event.EventHandler;
import org.springframework.util.ClassUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 通过反射解析 {@link EventHandler} 的具体事件类型。
 *
 * <p>典型写法是 {@code implements EventHandler<OrderPaidEvent>}，解析器会检查泛型接口
 * （以及父类接口）以获得具体的 {@link DomainEvent} 子类，用于路由。</p>
 */
public final class EventHandlerTypeResolver {

    private EventHandlerTypeResolver() {
    }

    /**
     * 获取 handler 对应的事件类型。
     *
     * @param handler 可能被代理的 handler Bean
     * @return 事件类型 Class，无法解析时返回 null
     */
    public static Class<? extends DomainEvent> resolveEventType(final EventHandler<?> handler) {
        final Class<?> targetClass = ClassUtils.getUserClass(handler);
        Class<? extends DomainEvent> resolved = resolveFromType(targetClass);
        if (resolved != null) {
            return resolved;
        }
        Class<?> superClass = targetClass.getSuperclass();
        while (superClass != null && !Object.class.equals(superClass)) {
            resolved = resolveFromType(superClass);
            if (resolved != null) {
                return resolved;
            }
            superClass = superClass.getSuperclass();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends DomainEvent> resolveFromType(final Class<?> type) {
        for (Type genericInterface : type.getGenericInterfaces()) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                if (EventHandler.class.equals(parameterizedType.getRawType())) {
                    Type eventType = parameterizedType.getActualTypeArguments()[0];
                    if (eventType instanceof Class<?> eventClass) {
                        return (Class<? extends DomainEvent>) eventClass;
                    }
                    if (eventType instanceof ParameterizedType parameterizedEvent) {
                        Type rawType = parameterizedEvent.getRawType();
                        if (rawType instanceof Class<?>) {
                            return (Class<? extends DomainEvent>) rawType;
                        }
                    }
                }
            }
        }
        return null;
    }
}
