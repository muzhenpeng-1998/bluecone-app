package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.member.api.dto.PointsBalanceDTO;
import com.bluecone.app.member.api.facade.MemberQueryFacade;
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
 * Stage5: 积分抵扣阶段
 * 调用 PointsQuery/规则折算，输出 line
 */
@Component
public class PointsStage implements PricingStage {
    
    private static final Logger log = LoggerFactory.getLogger(PointsStage.class);
    
    /**
     * 积分兑换比例：100积分 = 1元
     */
    private static final BigDecimal POINTS_TO_MONEY_RATE = new BigDecimal("0.01");
    
    /**
     * 积分抵扣上限：订单金额的50%
     */
    private static final BigDecimal MAX_POINTS_DISCOUNT_RATE = new BigDecimal("0.5");
    
    private final MemberQueryFacade memberQueryFacade;
    
    public PointsStage(MemberQueryFacade memberQueryFacade) {
        this.memberQueryFacade = memberQueryFacade;
    }
    
    @Override
    public void execute(PricingContext context) {
        log.debug("Executing PointsStage");
        
        Integer usePoints = context.getRequest().getUsePoints();
        if (usePoints == null || usePoints <= 0) {
            log.debug("No points specified, skipping PointsStage");
            return;
        }
        
        Long memberId = context.getRequest().getMemberId();
        if (memberId == null) {
            log.warn("MemberId is null, cannot use points");
            context.putContextData("points_unavailable_reason", "非会员不能使用积分");
            return;
        }
        
        try {
            // 查询会员积分余额
            PointsBalanceDTO pointsBalance = memberQueryFacade.getPointsBalance(
                    context.getRequest().getTenantId(), 
                    memberId
            );
            
            if (pointsBalance == null || pointsBalance.getAvailablePoints() < usePoints) {
                log.warn("Insufficient points: available={}, requested={}", 
                        pointsBalance != null ? pointsBalance.getAvailablePoints() : 0, usePoints);
                context.putContextData("points_unavailable_reason", "积分余额不足");
                return;
            }
            
            // 计算积分抵扣金额
            BigDecimal pointsDiscountAmount = BigDecimal.valueOf(usePoints)
                    .multiply(POINTS_TO_MONEY_RATE)
                    .setScale(2, RoundingMode.DOWN);
            
            // 计算积分抵扣上限（订单金额的50%）
            BigDecimal maxPointsDiscount = context.getCurrentAmount()
                    .multiply(MAX_POINTS_DISCOUNT_RATE)
                    .setScale(2, RoundingMode.DOWN);
            
            // 确保不超过上限
            if (pointsDiscountAmount.compareTo(maxPointsDiscount) > 0) {
                pointsDiscountAmount = maxPointsDiscount;
                // 重新计算实际使用的积分数
                usePoints = pointsDiscountAmount.divide(POINTS_TO_MONEY_RATE, 0, RoundingMode.DOWN).intValue();
                log.debug("Points discount capped at max: maxDiscount={}, actualPoints={}", 
                        maxPointsDiscount, usePoints);
            }
            
            // 确保不超过当前金额
            if (pointsDiscountAmount.compareTo(context.getCurrentAmount()) > 0) {
                pointsDiscountAmount = context.getCurrentAmount();
                usePoints = pointsDiscountAmount.divide(POINTS_TO_MONEY_RATE, 0, RoundingMode.DOWN).intValue();
            }
            
            if (pointsDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
                // 添加积分抵扣明细行
                context.addBreakdownLine(new PricingLine(
                        ReasonCode.POINTS_DISCOUNT,
                        String.format("积分抵扣: %d积分", usePoints),
                        pointsDiscountAmount.negate(),
                        memberId,
                        "POINTS"
                ));
                
                // 更新上下文
                context.setPointsDiscountAmount(pointsDiscountAmount);
                context.subtractAmount(pointsDiscountAmount);
                context.setAppliedPoints(usePoints);
                
                log.debug("PointsStage completed: points={}, discountAmount={}", 
                        usePoints, pointsDiscountAmount);
            }
        } catch (Exception e) {
            log.error("Error in PointsStage", e);
            context.putContextData("points_error", e.getMessage());
        }
    }
    
    @Override
    public String getStageName() {
        return "PointsStage";
    }
    
    @Override
    public int getOrder() {
        return 5;
    }
}
