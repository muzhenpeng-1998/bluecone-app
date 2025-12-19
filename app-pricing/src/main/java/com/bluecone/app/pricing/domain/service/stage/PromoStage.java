package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.pricing.domain.model.PricingContext;
import com.bluecone.app.pricing.domain.service.PricingStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stage3: 活动折扣阶段
 * 应用活动折扣（预留接口，暂不改价）
 */
@Component
public class PromoStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(PromoStage.class);
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing PromoStage (placeholder, no discount applied)");
        
        // TODO: 预留接口，后续实现活动折扣逻辑
        // 1. 根据商品、门店、时间查询可用活动
        // 2. 计算活动折扣金额
        // 3. 添加 PROMO_DISCOUNT 明细行
        // 4. 更新 context.promoDiscountAmount 和 context.currentAmount
        
        log.debug("PromoStage completed (no-op)");
    }
    
    @Override
    public String getStageName() {
        return "PromoStage";
    }
    
    @Override
    public int getOrder() {
        return 3;
    }
}
