package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 钱包操作汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletSection {
    
    /**
     * 钱包冻结记录列表
     */
    private List<WalletFreezeItem> freezes;
    
    /**
     * 钱包流水记录列表
     */
    private List<WalletLedgerItem> ledgers;
    
    /**
     * 记录总数（用于判断是否被截断）
     */
    private Integer totalCount;
}
