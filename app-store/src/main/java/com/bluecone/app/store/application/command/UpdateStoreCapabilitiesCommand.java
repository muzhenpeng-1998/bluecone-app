package com.bluecone.app.store.application.command;

import com.bluecone.app.store.domain.model.StoreCapabilityModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新门店能力列表的写侧命令，支持批量开关能力。
 * <p>高并发场景下依赖 configVersion 做乐观锁保护。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreCapabilitiesCommand {
    private Long tenantId;
    private Long storeId;
    private List<StoreCapabilityModel> capabilities;
    private Long expectedConfigVersion;
}
