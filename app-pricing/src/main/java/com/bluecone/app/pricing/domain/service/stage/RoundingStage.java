package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.pricing.api.dto.PricingLine;
import com.bluecone.app.pricing.api.enums.ReasonCode;
import com.bluecone.app.pricing.domain.model.PricingContext;
import com.bluecone.app.pricing.domain.service.PricingStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Stage7: 抹零阶段
 * 应用抹零规则（四舍五入到角）
 */
@Component
public class RoundingStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(RoundingStage.class);
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing RoundingStage");
        
        Boolean enableRounding = context.getRequest().getEnableRounding();
        if (!Boolean.TRUE.equals(enableRounding)) {
            log.debug("Rounding disabled, skipping RoundingStage");
            return;
        }
        
        BigDecimal currentAmount = context.getCurrentAmount();
        
        // 四舍五入到角（保留1位小数）
        BigDecimal roundedAmount = currentAmount.setScale(1, RoundingMode.HALF_UP);
        
        // 计算抹零金额
        BigDecimal roundingAmount = roundedAmount.subtract(currentAmount);
        
        if (roundingAmount.compareTo(BigDecimal.ZERO) != 0) {
            // 添加抹零明细行
            context.addBreakdownLine(new PricingLine(
                    ReasonCode.ROUNDING,
                    "抹零",
                    roundingAmount
            ));
            
            context.setRoundingAmount(roundingAmount);
            context.setCurrentAmount(roundedAmount);
            
            log.debug("RoundingStage completed: roundingAmount={}, finalAmount={}", 
                    roundingAmount, roundedAmount);
        } else {
            log.debug("RoundingStage completed: no rounding needed");
        }
    }
    
    @Override
    public String getStageName() {
        return "RoundingStage";
    }
    
    @Override
    public int getOrder() {
        return 7;
    }
}
