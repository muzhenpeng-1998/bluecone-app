package com.bluecone.app.growth.api.dto;

import com.bluecone.app.growth.api.enums.RewardType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 奖励配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardConfig {
    
    /**
     * 奖励类型
     */
    private RewardType type;
    
    /**
     * 奖励值（根据类型不同而不同）
     * COUPON: templateId（优惠券模板ID）
     * WALLET_CREDIT: amount（金额，单位：分）
     * POINTS: points（积分数）
     */
    private String value;
    
    /**
     * 奖励描述
     */
    private String description;
}
