package com.bluecone.app.core.notify;

import java.util.Objects;

/**
 * 通用通知场景枚举（API 层）。
 *
 * <p>枚举保持有限且语义化，便于配置中心、策略路由与模板绑定。</p>
 */
public enum NotificationScenario {

    ORDER_PAID_SHOP_OWNER("order.paid.shop-owner", NotificationPriority.HIGH, "订单支付通知店主"),

    ORDER_PAID_BARISTA("order.paid.barista", NotificationPriority.NORMAL, "订单支付通知咖啡师"),

    INVENTORY_LOW_SHOP_OWNER("inventory.low.shop-owner", NotificationPriority.NORMAL, "库存低通知店主"),

    SYSTEM_ERROR_PLATFORM_OPS("system.error.platform-ops", NotificationPriority.HIGH, "平台异常通知运维");

    private final String code;
    private final NotificationPriority defaultPriority;
    private final String description;

    NotificationScenario(String code, NotificationPriority defaultPriority, String description) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.defaultPriority = defaultPriority == null ? NotificationPriority.NORMAL : defaultPriority;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public NotificationPriority getDefaultPriority() {
        return defaultPriority;
    }

    public String getDescription() {
        return description;
    }

    public static NotificationScenario fromCode(String code) {
        for (NotificationScenario scenario : values()) {
            if (scenario.code.equals(code)) {
                return scenario;
            }
        }
        return null;
    }
}
