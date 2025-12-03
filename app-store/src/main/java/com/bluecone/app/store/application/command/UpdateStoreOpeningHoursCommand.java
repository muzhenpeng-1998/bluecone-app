package com.bluecone.app.store.application.command;

import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新常规营业时间配置的命令对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreOpeningHoursCommand {
    private Long tenantId;
    private Long storeId;
    private StoreOpeningSchedule schedule;
    private Long expectedConfigVersion;
}
