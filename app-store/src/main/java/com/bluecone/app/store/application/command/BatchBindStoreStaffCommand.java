package com.bluecone.app.store.application.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量绑定门店员工的命令（如总部导入）。
 * <p>高并发：一次提交多条关系，避免多次远端调用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchBindStoreStaffCommand {

    private Long tenantId;
    private Long storeId;
    private List<StaffItem> staffList;
    private Long operatorId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffItem {
        private Long userId;
        private String role;
    }
}
