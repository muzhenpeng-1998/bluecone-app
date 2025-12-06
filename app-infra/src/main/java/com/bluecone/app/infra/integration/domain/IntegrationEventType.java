package com.bluecone.app.infra.integration.domain;

/**
 * 常用领域事件类型常量，便于订阅配置与匹配。
 *
 * <p>事件类型仍支持自由字符串，业务可按需扩展，不强制使用本枚举常量。</p>
 */
public final class IntegrationEventType {

    private IntegrationEventType() {
    }

    public static final String ORDER_PAID = "order.paid";
    public static final String ORDER_CREATED = "order.created";
    public static final String PAYMENT_SUCCESS = "payment.success";
    public static final String USER_CREATED = "user.created";
}
