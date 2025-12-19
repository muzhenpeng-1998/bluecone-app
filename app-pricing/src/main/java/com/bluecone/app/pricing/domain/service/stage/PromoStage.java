package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.campaign.api.dto.CampaignDTO;
import com.bluecone.app.campaign.api.dto.CampaignQueryContext;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.api.facade.CampaignQueryFacade;
import io.micrometer.core.instrument.MeterRegistry;
import com.bluecone.app.pricing.api.dto.PricingLine;
import com.bluecone.app.pricing.api.enums.ReasonCode;
import com.bluecone.app.pricing.domain.model.PricingContext;
import com.bluecone.app.pricing.domain.service.PricingStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Stage3: 活动折扣阶段
 * 应用活动折扣（ORDER_DISCOUNT 类型活动）
 */
@Component
public class PromoStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(PromoStage.class);
    
    @Autowired(required = false)
    private CampaignQueryFacade campaignQueryFacade;
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing PromoStage");
        
        // 如果没有注入 CampaignQueryFacade，跳过（向后兼容）
        if (campaignQueryFacade == null) {
            log.debug("PromoStage skipped: CampaignQueryFacade not available");
            return;
        }
        
        try {
            // 1. 查询可用的订单满减活动
            CampaignQueryContext queryContext = CampaignQueryContext.builder()
                    .tenantId(context.getRequest().getTenantId())
                    .campaignType(CampaignType.ORDER_DISCOUNT)
                    .storeId(context.getRequest().getStoreId())
                    .userId(context.getRequest().getUserId())
                    .amount(context.getCurrentAmount())
                    .build();
            
            List<CampaignDTO> campaigns = campaignQueryFacade.queryAvailableCampaigns(queryContext);
            
            if (campaigns == null || campaigns.isEmpty()) {
                log.debug("PromoStage: no available campaigns");
                return;
            }
            
            // 2. 应用第一个匹配的活动（已按优先级排序）
            CampaignDTO campaign = campaigns.get(0);
            BigDecimal discount = calculateDiscount(context.getCurrentAmount(), campaign);
            
            if (discount.compareTo(BigDecimal.ZERO) > 0) {
                // 3. 添加活动折扣明细行
                PricingLine line = new PricingLine(
                        ReasonCode.PROMO_DISCOUNT,
                        campaign.getCampaignName(),
                        discount.negate(),
                        campaign.getId(),
                        "CAMPAIGN"
                );
                context.addBreakdownLine(line);
                
                // 4. 更新上下文
                context.setPromoDiscountAmount(discount);
                context.subtractAmount(discount);
                
                // 5. 记录活动ID（用于订单快照）
                context.putContextData("appliedCampaignId", campaign.getId());
                context.putContextData("appliedCampaignCode", campaign.getCampaignCode());
                
                // 6. 记录指标
                recordCampaignApplied(campaign);
                
                log.info("PromoStage: applied campaign {}, discount={}", 
                        campaign.getCampaignCode(), discount);
            }
            
        } catch (Exception e) {
            // 活动系统异常不影响计价，只记录日志
            log.error("PromoStage: error querying campaigns", e);
        }
        
        log.debug("PromoStage completed");
    }
    
    /**
     * 计算活动折扣金额
     */
    private BigDecimal calculateDiscount(BigDecimal orderAmount, CampaignDTO campaign) {
        if (campaign.getRules() == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal discount = BigDecimal.ZERO;
        
        // 固定满减金额
        if (campaign.getRules().getDiscountAmount() != null) {
            discount = campaign.getRules().getDiscountAmount();
        }
        
        // 折扣率
        if (campaign.getRules().getDiscountRate() != null) {
            BigDecimal rate = campaign.getRules().getDiscountRate();
            BigDecimal rateDiscount = orderAmount.multiply(BigDecimal.ONE.subtract(rate))
                    .setScale(2, RoundingMode.DOWN);
            discount = discount.add(rateDiscount);
        }
        
        // 封顶
        if (campaign.getRules().getMaxDiscountAmount() != null 
                && discount.compareTo(campaign.getRules().getMaxDiscountAmount()) > 0) {
            discount = campaign.getRules().getMaxDiscountAmount();
        }
        
        // 优惠不能超过订单金额
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }
        
        return discount;
    }
    
    @Override
    public String getStageName() {
        return "PromoStage";
    }
    
    @Override
    public int getOrder() {
        return 3;
    }
    
    /**
     * 记录活动应用指标
     */
    private void recordCampaignApplied(CampaignDTO campaign) {
        if (meterRegistry != null) {
            try {
                meterRegistry.counter("campaign.applied.total",
                        "type", campaign.getCampaignType().name(),
                        "code", campaign.getCampaignCode())
                        .increment();
            } catch (Exception e) {
                log.warn("Failed to record campaign metrics", e);
            }
        }
    }
}
