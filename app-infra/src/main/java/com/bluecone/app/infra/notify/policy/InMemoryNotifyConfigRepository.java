package com.bluecone.app.infra.notify.policy;

import com.bluecone.app.core.notify.NotificationScenario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于内存的场景配置实现，便于快速落地与单测。
 *
 * <p>未来可替换为 ConfigCenter/DB 实现，只需满足 {@link NotifyConfigRepository} 接口。</p>
 */
public class InMemoryNotifyConfigRepository implements NotifyConfigRepository {

    private final Map<Long, Map<String, NotifyScenarioConfig>> tenantConfigs = new HashMap<>();

    public InMemoryNotifyConfigRepository() {
        seedDefaults();
    }

    @Override
    public NotifyScenarioConfig findScenarioConfig(Long tenantId, String scenarioCode) {
        Map<String, NotifyScenarioConfig> scoped = tenantConfigs.get(tenantId);
        NotifyScenarioConfig config = scoped != null ? scoped.get(scenarioCode) : null;
        if (config == null) {
            Map<String, NotifyScenarioConfig> global = tenantConfigs.get(null);
            config = global != null ? global.get(scenarioCode) : null;
        }
        return config;
    }

    private void seedDefaults() {
        // 全局默认：订单支付通知走企业微信机器人
        register(null, buildWeChatScenario(NotificationScenario.ORDER_PAID_SHOP_OWNER.getCode(), "wechat.order.paid.shop-owner"));
        register(null, buildWeChatScenario(NotificationScenario.ORDER_PAID_BARISTA.getCode(), "wechat.order.paid.barista"));
        register(null, buildWeChatScenario(NotificationScenario.SYSTEM_ERROR_PLATFORM_OPS.getCode(), "wechat.system.error"));
    }

    private void register(Long tenantId, NotifyScenarioConfig config) {
        tenantConfigs.computeIfAbsent(tenantId, key -> new HashMap<>()).put(config.getScenarioCode(), config);
    }

    private NotifyScenarioConfig buildWeChatScenario(String scenarioCode, String templateCode) {
        Objects.requireNonNull(scenarioCode, "scenarioCode must not be null");
        NotifyChannelConfig channelConfig = new NotifyChannelConfig();
        channelConfig.setChannel(NotifyChannel.WECHAT_BOT);
        channelConfig.setTemplateCode(templateCode);
        channelConfig.setMaxPerMinute(120);

        NotifyScenarioConfig config = new NotifyScenarioConfig();
        config.setScenarioCode(scenarioCode);
        config.setEnabled(true);
        config.setChannels(List.of(channelConfig));
        return config;
    }
}
