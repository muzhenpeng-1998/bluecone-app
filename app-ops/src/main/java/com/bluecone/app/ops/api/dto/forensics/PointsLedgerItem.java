package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 积分流水记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsLedgerItem {
    
    /**
     * 流水ID
     */
    private Long id;
    
    /**
     * 会员ID
     */
    private Long memberId;
    
    /**
     * 变动方向：IN/OUT
     */
    private String direction;
    
    /**
     * 变动积分值
     */
    private Long deltaPoints;
    
    /**
     * 变动前可用积分
     */
    private Long beforeAvailable;
    
    /**
     * 变动前冻结积分
     */
    private Long beforeFrozen;
    
    /**
     * 变动后可用积分
     */
    private Long afterAvailable;
    
    /**
     * 变动后冻结积分
     */
    private Long afterFrozen;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private String bizId;
    
    /**
     * 幂等键
     */
    private String idempotencyKey;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
