package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.pricing.domain.model.PricingContext;
import com.bluecone.app.pricing.domain.service.PricingStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stage2: 会员价阶段
 * 应用会员价/时段价（预留接口，暂不改价）
 */
@Component
public class MemberPriceStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(MemberPriceStage.class);
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing MemberPriceStage (placeholder, no discount applied)");
        
        // TODO: 预留接口，后续实现会员价逻辑
        // 1. 根据 memberId 查询会员等级
        // 2. 根据会员等级和商品查询会员价
        // 3. 如果有会员价，添加 MEMBER_PRICE 明细行
        // 4. 更新 context.memberDiscountAmount 和 context.currentAmount
        
        log.debug("MemberPriceStage completed (no-op)");
    }
    
    @Override
    public String getStageName() {
        return "MemberPriceStage";
    }
    
    @Override
    public int getOrder() {
        return 2;
    }
}
