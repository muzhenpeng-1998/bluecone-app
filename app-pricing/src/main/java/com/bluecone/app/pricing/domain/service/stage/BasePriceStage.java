package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.pricing.api.dto.PricingItem;
import com.bluecone.app.pricing.api.dto.PricingLine;
import com.bluecone.app.pricing.api.enums.ReasonCode;
import com.bluecone.app.pricing.domain.model.PricingContext;
import com.bluecone.app.pricing.domain.service.PricingStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stage1: 基价计算阶段
 * 计算商品基价和规格加价
 */
@Component
public class BasePriceStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(BasePriceStage.class);
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing BasePriceStage");
        
        BigDecimal totalBasePrice = BigDecimal.ZERO;
        BigDecimal totalSpecSurcharge = BigDecimal.ZERO;
        
        for (PricingItem item : context.getRequest().getItems()) {
            // 计算基价
            BigDecimal itemBasePrice = item.getBasePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalBasePrice = totalBasePrice.add(itemBasePrice);
            
            // 添加基价明细行
            context.addBreakdownLine(new PricingLine(
                    ReasonCode.BASE_PRICE,
                    String.format("%s x %d", item.getSkuName(), item.getQuantity()),
                    itemBasePrice,
                    item.getSkuId(),
                    "SKU"
            ));
            
            // 计算规格加价
            if (item.getSpecSurcharge() != null && item.getSpecSurcharge().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal itemSpecSurcharge = item.getSpecSurcharge()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                totalSpecSurcharge = totalSpecSurcharge.add(itemSpecSurcharge);
                
                // 添加规格加价明细行
                context.addBreakdownLine(new PricingLine(
                        ReasonCode.SPEC_SURCHARGE,
                        String.format("%s 规格加价 x %d", item.getSkuName(), item.getQuantity()),
                        itemSpecSurcharge,
                        item.getSkuId(),
                        "SKU"
                ));
            }
        }
        
        // 更新上下文
        BigDecimal originalAmount = totalBasePrice.add(totalSpecSurcharge);
        context.setOriginalAmount(originalAmount);
        context.setCurrentAmount(originalAmount);
        
        log.debug("BasePriceStage completed: originalAmount={}", originalAmount);
    }
    
    @Override
    public String getStageName() {
        return "BasePriceStage";
    }
    
    @Override
    public int getOrder() {
        return 1;
    }
}
