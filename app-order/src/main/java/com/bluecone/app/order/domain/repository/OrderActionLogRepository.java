package com.bluecone.app.order.domain.repository;

import com.bluecone.app.order.domain.model.OrderActionLog;

/**
 * 订单动作幂等日志仓储接口。
 */
public interface OrderActionLogRepository {

    /**
     * 根据 actionKey 查询动作日志（用于幂等判断）。
     * 
     * @param tenantId 租户ID
     * @param actionKey 幂等唯一键
     * @return 动作日志，不存在返回 null
     */
    OrderActionLog findByActionKey(Long tenantId, String actionKey);

    /**
     * 保存动作日志（用于首次创建幂等记录）。
     * <p>如果 actionKey 已存在，会抛出唯一约束异常（由调用方处理）。</p>
     * 
     * @param log 动作日志
     */
    void save(OrderActionLog log);

    /**
     * 更新动作日志（用于更新执行结果）。
     * 
     * @param log 动作日志
     */
    void update(OrderActionLog log);
}
