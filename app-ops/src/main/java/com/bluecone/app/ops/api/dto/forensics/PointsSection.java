package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 积分操作汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsSection {
    
    /**
     * 积分流水记录列表
     */
    private List<PointsLedgerItem> ledgers;
    
    /**
     * 记录总数（用于判断是否被截断）
     */
    private Integer totalCount;
}
