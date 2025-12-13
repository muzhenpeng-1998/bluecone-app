package com.bluecone.app.store.api.dto;

import com.bluecone.app.core.domain.IndustryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * 下单场景使用的门店快照，仅包含订单链路需要的信息。
 * <p>高并发：依赖 StoreConfig 的整体快照生成，减少多次表查询；缓存时会附带 configVersion 做版本化。</p>
 * <p>高隔离：外部模块只消费快照，不直接接触领域模型。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderSnapshot {

    private Long tenantId;
    /**
     * 对外门店 ID（PublicId），供上层透传使用。
     */
    private String storePublicId;
    /**
     * 内部门店 ID（仅内部使用，外部不应依赖）。
     */
    private Long storeId;
    private String storeName;
    private String cityCode;
    private IndustryType industryType;
    /** 业务状态（示例：1=营业，0=关闭，-1=审核中），待统一枚举 */
    private Integer bizStatus;
    private String status;
    private Boolean openForOrders;

    /**
     * 已启用的能力集合。
     */
    private Set<String> enabledCapabilities;

    /**
     * 当前时间是否处于营业状态（可选字段，后续在领域服务中填充）。
     */
    private Boolean currentlyOpen;

    /**
     * 是否当前可接单（结合运行时判断）。
     */
    private Boolean canAcceptOrder;

    /** 能力预留：外卖/自提/堂食。 */
    private Boolean takeoutEnabled;
    private Boolean pickupEnabled;
    private Boolean dineInEnabled;

    /**
     * 是否命中特殊日配置。
     */
    private Boolean specialDayHit;

    /**
     * 当前渠道的绑定状态摘要。
     */
    private String channelType;
    private String channelStatus;

    /**
     * 配置版本号，用于缓存一致性校验。
     */
    private Long configVersion;
}
