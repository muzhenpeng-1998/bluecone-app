package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 门店完整配置聚合根，聚合 bc_store 及多张子表后的领域模型。
 * <p>高隔离：仅在领域层内部流转，对外通过 StoreContextProvider 暴露语义化接口，屏蔽底层表结构与 Mapper。</p>
 * <p>高稳定：可基于 configVersion 做版本化快照，结合多级缓存实现降级（DB/Redis），当前仅预留扩展点。</p>
 * <p>高并发：StoreConfig 会作为整体快照放入缓存，避免高并发场景下的多次分表查询，提高命中率与隔离度。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreConfig {

    /**
     * 租户 ID，用于多租户隔离。
     */
    private Long tenantId;

    /**
     * 门店主键，等同于 bc_store.id。
     */
    private Long storeId;

    /**
     * 门店编码，通常对外展示使用。
     */
    private String storeCode;

    /**
     * 门店名称。
     */
    private String name;

    /**
     * 门店简称。
     */
    private String shortName;

    /**
     * 行业类型，用于后续场景扩展（餐饮、零售、场馆等）。
     */
    private String industryType;

    /**
     * 城市编码，部分区域逻辑或运力逻辑会依赖。
     */
    private String cityCode;

    /**
     * 门店业务状态，例如 OPEN/CLOSED。
     */
    private String status;

    /**
     * 是否可接单（配置维度开关）。
     */
    private Boolean openForOrders;

    /**
     * 配置版本号，乐观锁与缓存版本化的核心字段。
     */
    private Long configVersion;

    /**
     * 能力列表（堂食/外卖/自取/预约等）。
     */
    private List<StoreCapabilityModel> capabilities;

    /**
     * 营业时间与特殊日配置。
     */
    private StoreOpeningSchedule openingSchedule;

    /**
     * 渠道绑定信息。
     */
    private List<StoreChannelModel> channels;

    /**
     * 资源（桌台、房间、场馆资源等）。
     */
    private List<StoreResourceModel> resources;

    /**
     * 设备列表（包含打印规则）。
     */
    private List<StoreDeviceModel> devices;

    /**
     * 员工列表。
     */
    private List<StoreStaffModel> staff;
}
