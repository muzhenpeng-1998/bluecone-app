package com.bluecone.app.pricing.domain.service;

import com.bluecone.app.pricing.domain.model.PricingContext;

/**
 * 计价阶段接口
 * 每个 Stage 负责一个独立的计价逻辑
 * 只操作 PricingContext，确保可测试和可扩展
 */
public interface PricingStage {
    
    /**
     * 执行计价阶段
     * 
     * @param context 计价上下文
     */
    void execute(PricingContext context);
    
    /**
     * 获取阶段名称
     * 
     * @return 阶段名称
     */
    String getStageName();
    
    /**
     * 获取阶段顺序（越小越先执行）
     * 
     * @return 顺序号
     */
    int getOrder();
}
