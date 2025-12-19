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
 * Stage6: 费用阶段
 * 计算配送费和打包费
 */
@Component
public class FeeStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(FeeStage.class);
    
    /**
     * 配送费起步价（3公里内）
     */
    private static final BigDecimal DELIVERY_BASE_FEE = new BigDecimal("5.00");
    
    /**
     * 配送费每公里加价（超过3公里）
     */
    private static final BigDecimal DELIVERY_PER_KM_FEE = new BigDecimal("2.00");
    
    /**
     * 配送费起步距离（公里）
     */
    private static final BigDecimal DELIVERY_BASE_DISTANCE = new BigDecimal("3.0");
    
    /**
     * 打包费（固定）
     */
    private static final BigDecimal PACKING_FEE = new BigDecimal("1.00");
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing FeeStage");
        
        String deliveryMode = context.getRequest().getDeliveryMode();
        
        // 计算配送费
        if ("DELIVERY".equalsIgnoreCase(deliveryMode)) {
            BigDecimal deliveryDistance = context.getRequest().getDeliveryDistance();
            if (deliveryDistance == null) {
                deliveryDistance = BigDecimal.ZERO;
            }
            
            BigDecimal deliveryFee = calculateDeliveryFee(deliveryDistance);
            if (deliveryFee.compareTo(BigDecimal.ZERO) > 0) {
                // 添加配送费明细行
                context.addBreakdownLine(new PricingLine(
                        ReasonCode.DELIVERY_FEE,
                        String.format("配送费 (%.1f公里)", deliveryDistance),
                        deliveryFee
                ));
                
                context.setDeliveryFee(deliveryFee);
                context.addAmount(deliveryFee);
            }
        }
        
        // 计算打包费
        BigDecimal packingFee = PACKING_FEE;
        context.addBreakdownLine(new PricingLine(
                ReasonCode.PACKING_FEE,
                "打包费",
                packingFee
        ));
        
        context.setPackingFee(packingFee);
        context.addAmount(packingFee);
        
        log.debug("FeeStage completed: deliveryFee={}, packingFee={}", 
                context.getDeliveryFee(), context.getPackingFee());
    }
    
    /**
     * 计算配送费
     */
    private BigDecimal calculateDeliveryFee(BigDecimal distance) {
        if (distance.compareTo(DELIVERY_BASE_DISTANCE) <= 0) {
            // 3公里内，起步价
            return DELIVERY_BASE_FEE;
        } else {
            // 超过3公里，每公里加价
            BigDecimal extraDistance = distance.subtract(DELIVERY_BASE_DISTANCE);
            BigDecimal extraFee = extraDistance.multiply(DELIVERY_PER_KM_FEE)
                    .setScale(2, RoundingMode.UP);
            return DELIVERY_BASE_FEE.add(extraFee);
        }
    }
    
    @Override
    public String getStageName() {
        return "FeeStage";
    }
    
    @Override
    public int getOrder() {
        return 6;
    }
}
