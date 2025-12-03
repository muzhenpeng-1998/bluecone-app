package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 切换「可接单」开关的写侧命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleOpenForOrdersCommand {
    private Long tenantId;
    private Long storeId;
    private Boolean openForOrders;
    private Long expectedConfigVersion;
}
