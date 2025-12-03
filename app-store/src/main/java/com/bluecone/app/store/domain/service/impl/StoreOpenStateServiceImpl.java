package com.bluecone.app.store.domain.service.impl;

import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.domain.model.StoreConfig;
import com.bluecone.app.store.domain.service.StoreOpenStateService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 门店营业状态校验实现，集中封装接单规则。
 * <p>高稳定：为后续降级预留扩展点，例如 DB 异常时仍可基于缓存快照判断。</p>
 * <p>高并发：直接依赖 StoreConfig 快照，无需重复查询子表。</p>
 */
@Service
public class StoreOpenStateServiceImpl implements StoreOpenStateService {

    @Override
    public StoreOrderAcceptResult check(StoreConfig config, String capability, LocalDateTime now, String channelType) {
        // 1. 检查门店状态是否 OPEN
        // TODO: 根据 config.getStatus() 判定是否营业，否则返回 STORE_CLOSED
        // 2. 检查开关 openForOrders
        // TODO: openForOrders=false 时返回 STORE_ORDER_DISABLED
        // 3. 检查能力是否启用
        // TODO: capability 未启用时返回 CAPABILITY_DISABLED
        // 4. 检查特殊日配置（bc_store_special_day）
        // TODO: 若特殊日关闭/限时，应优先返回特殊日原因
        // 5. 检查常规营业时间（bc_store_opening_hours）
        // TODO: 使用 openingSchedule.isOpenAt(now) 判定
        // 6. 检查渠道绑定状态（bc_store_channel）
        // TODO: 渠道未绑定或禁用时返回 CHANNEL_BLOCKED
        // 以上步骤未来可结合缓存/熔断实现降级策略
        return StoreOrderAcceptResult.builder()
                .acceptable(true)
                .reasonCode(null)
                .reasonMessage(null)
                .build();
    }
}
