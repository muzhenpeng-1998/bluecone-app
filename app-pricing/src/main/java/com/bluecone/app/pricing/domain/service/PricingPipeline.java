package com.bluecone.app.pricing.domain.service;

import com.bluecone.app.pricing.api.dto.PricingQuote;
import com.bluecone.app.pricing.api.dto.PricingRequest;
import com.bluecone.app.pricing.domain.model.PricingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 计价流水线
 * 按顺序执行各个计价阶段，最终生成 PricingQuote
 */
@Service
public class PricingPipeline {
    
    private static final Logger log = LoggerFactory.getLogger(PricingPipeline.class);
    
    private final List<PricingStage> stages;
    
    public PricingPipeline(List<PricingStage> stages) {
        // 按 order 排序
        this.stages = stages.stream()
                .sorted(Comparator.comparingInt(PricingStage::getOrder))
                .toList();
        
        log.info("PricingPipeline initialized with {} stages: {}", 
                stages.size(), 
                stages.stream().map(PricingStage::getStageName).toList());
    }
    
    /**
     * 执行计价流水线
     * 
     * @param request 计价请求
     * @return 计价报价单
     */
    public PricingQuote execute(PricingRequest request) {
        log.debug("Starting pricing pipeline for tenantId={}, storeId={}, userId={}", 
                request.getTenantId(), request.getStoreId(), request.getUserId());
        
        // 创建计价上下文
        PricingContext context = new PricingContext(request);
        
        // 依次执行各个阶段
        for (PricingStage stage : stages) {
            try {
                log.debug("Executing stage: {}", stage.getStageName());
                stage.execute(context);
            } catch (Exception e) {
                log.error("Error executing stage: {}", stage.getStageName(), e);
                context.setUnavailableReason("计价失败: " + stage.getStageName() + " - " + e.getMessage());
                break;
            }
        }
        
        // 构建报价单
        PricingQuote quote = buildQuote(context);
        
        log.debug("Pricing pipeline completed. PayableAmount={}, BreakdownLines={}", 
                quote.getPayableAmount(), quote.getBreakdownLines().size());
        
        return quote;
    }
    
    /**
     * 从上下文构建报价单
     */
    private PricingQuote buildQuote(PricingContext context) {
        PricingQuote quote = new PricingQuote();
        
        // 生成报价单ID
        quote.setQuoteId(UUID.randomUUID().toString().replace("-", ""));
        quote.setPricingVersion("1.0.0");
        quote.setPricingTime(LocalDateTime.now());
        
        // 设置金额
        quote.setOriginalAmount(context.getOriginalAmount());
        quote.setMemberDiscountAmount(context.getMemberDiscountAmount());
        quote.setPromoDiscountAmount(context.getPromoDiscountAmount());
        quote.setCouponDiscountAmount(context.getCouponDiscountAmount());
        quote.setPointsDiscountAmount(context.getPointsDiscountAmount());
        quote.setDeliveryFee(context.getDeliveryFee());
        quote.setPackingFee(context.getPackingFee());
        quote.setRoundingAmount(context.getRoundingAmount());
        quote.setPayableAmount(context.getCurrentAmount());
        
        // 设置明细行
        quote.setBreakdownLines(context.getBreakdownLines());
        
        // 设置应用的优惠
        quote.setAppliedCouponId(context.getAppliedCouponId());
        quote.setAppliedPoints(context.getAppliedPoints());
        
        // 设置不可用原因
        quote.setUnavailableReason(context.getUnavailableReason());
        
        return quote;
    }
}
