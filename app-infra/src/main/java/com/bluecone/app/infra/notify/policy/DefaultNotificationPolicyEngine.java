package com.bluecone.app.infra.notify.policy;

import com.bluecone.app.infra.notify.model.NotificationIntent;
import com.bluecone.app.infra.notify.model.NotificationPlan;
import com.bluecone.app.infra.notify.model.NotificationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 默认路由与策略引擎实现。
 *
 * <p>职责：
 * 1. 按租户+场景加载配置；
 * 2. 未配置或关闭直接返回空计划（API 返回 rejected）；
 * 3. 生成通道任务并附带通道级幂等 Key。</p>
 */
public class DefaultNotificationPolicyEngine implements NotificationPolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationPolicyEngine.class);

    private final NotifyConfigRepository configRepository;

    public DefaultNotificationPolicyEngine(NotifyConfigRepository configRepository) {
        this.configRepository = Objects.requireNonNull(configRepository, "configRepository must not be null");
    }

    @Override
    public NotificationPlan evaluate(NotificationIntent intent) {
        Objects.requireNonNull(intent, "intent must not be null");
        NotifyScenarioConfig config = configRepository.findScenarioConfig(intent.getTenantId(), intent.getScenarioCode());
        if (config == null || !config.isEnabled() || CollectionUtils.isEmpty(config.getChannels())) {
            log.warn("[NotifyPolicy] scenario disabled or missing config scenario={} tenantId={}", intent.getScenarioCode(), intent.getTenantId());
            return new NotificationPlan(intent, List.of());
        }
        List<NotificationTask> tasks = new ArrayList<>();
        for (NotifyChannelConfig channelConfig : config.getChannels()) {
            if (channelConfig.getChannel() == null) {
                continue;
            }
            String idempotentKey = buildIdempotentKey(intent, channelConfig);
            tasks.add(new NotificationTask(
                    channelConfig.getChannel(),
                    channelConfig.getTemplateCode(),
                    intent.getAttributes(),
                    intent.getPriority(),
                    idempotentKey,
                    intent.getTenantId(),
                    intent.getScenarioCode(),
                    channelConfig.getMaxPerMinute(),
                    channelConfig.getChannelConfigId()
            ));
        }
        return new NotificationPlan(intent, tasks);
    }

    private String buildIdempotentKey(NotificationIntent intent, NotifyChannelConfig channelConfig) {
        if (StringUtils.hasText(intent.getIdempotentKey())) {
            return intent.getIdempotentKey() + ":" + channelConfig.getChannel().getCode();
        }
        String tenantPart = intent.getTenantId() == null ? "global" : String.valueOf(intent.getTenantId());
        return String.format("%s:%s:%s", intent.getScenarioCode(), channelConfig.getChannel().getCode(), tenantPart);
    }
}
