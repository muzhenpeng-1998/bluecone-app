package com.bluecone.app.store.application.command;

import com.bluecone.app.store.domain.model.StoreOpeningSchedule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新特殊日配置的命令对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStoreSpecialDaysCommand {
    private Long tenantId;
    private Long storeId;
    private List<StoreOpeningSchedule.SpecialDayItem> specialDays;
    private Long expectedConfigVersion;
}
