package com.bluecone.app.infra.notify.policy;

/**
 * 通知配置仓储（Policy 层）。
 *
 * <p>当前可使用内存配置，未来对接 ConfigCenter/DB。</p>
 */
public interface NotifyConfigRepository {

    /**
     * 根据租户与场景查询配置。
     *
     * @param tenantId    租户 ID
     * @param scenarioCode 场景编码
     * @return 场景配置，可为空
     */
    NotifyScenarioConfig findScenarioConfig(Long tenantId, String scenarioCode);
}
