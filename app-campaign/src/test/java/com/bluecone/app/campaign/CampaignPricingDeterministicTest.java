package com.bluecone.app.campaign;

import com.bluecone.app.campaign.api.dto.CampaignRulesDTO;
import com.bluecone.app.campaign.domain.service.CampaignExecutionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 活动计价确定性测试
 * 
 * 测试场景：
 * 1. 同一规则、同一订单金额，多次计算应该得到相同结果
 * 2. 折扣计算的边界条件
 */
public class CampaignPricingDeterministicTest {
    
    private final CampaignExecutionService executionService = new CampaignExecutionService(
            null, null, null, null, null
    );
    
    /**
     * 测试：固定满减金额计算
     */
    @Test
    public void testFixedDiscountAmount_ShouldBeDeterministic() {
        // Given: 满50减10规则
        CampaignRulesDTO rules = CampaignRulesDTO.builder()
                .minAmount(new BigDecimal("50.00"))
                .discountAmount(new BigDecimal("10.00"))
                .build();
        
        BigDecimal orderAmount = new BigDecimal("150.00");
        
        // When: 多次计算
        BigDecimal discount1 = executionService.calculateOrderDiscount(orderAmount, rules);
        BigDecimal discount2 = executionService.calculateOrderDiscount(orderAmount, rules);
        BigDecimal discount3 = executionService.calculateOrderDiscount(orderAmount, rules);
        
        // Then: 结果应该完全一致
        assertEquals(new BigDecimal("10.00"), discount1);
        assertEquals(discount1, discount2);
        assertEquals(discount2, discount3);
    }
    
    /**
     * 测试：折扣率计算
     */
    @Test
    public void testDiscountRate_ShouldBeDeterministic() {
        // Given: 85折规则
        CampaignRulesDTO rules = CampaignRulesDTO.builder()
                .discountRate(new BigDecimal("0.85"))
                .build();
        
        BigDecimal orderAmount = new BigDecimal("100.00");
        
        // When: 多次计算
        BigDecimal discount1 = executionService.calculateOrderDiscount(orderAmount, rules);
        BigDecimal discount2 = executionService.calculateOrderDiscount(orderAmount, rules);
        
        // Then: 应该一致（15元折扣）
        assertEquals(new BigDecimal("15.00"), discount1);
        assertEquals(discount1, discount2);
    }
    
    /**
     * 测试：折扣封顶
     */
    @Test
    public void testMaxDiscountAmount_ShouldBeCapped() {
        // Given: 8折，最高优惠20元
        CampaignRulesDTO rules = CampaignRulesDTO.builder()
                .discountRate(new BigDecimal("0.80"))
                .maxDiscountAmount(new BigDecimal("20.00"))
                .build();
        
        // When: 订单金额150元（理论折扣30元）
        BigDecimal discount = executionService.calculateOrderDiscount(
                new BigDecimal("150.00"), rules
        );
        
        // Then: 应该封顶在20元
        assertEquals(new BigDecimal("20.00"), discount);
    }
    
    /**
     * 测试：折扣不能超过订单金额
     */
    @Test
    public void testDiscount_ShouldNotExceedOrderAmount() {
        // Given: 固定减100元
        CampaignRulesDTO rules = CampaignRulesDTO.builder()
                .discountAmount(new BigDecimal("100.00"))
                .build();
        
        // When: 订单金额只有50元
        BigDecimal discount = executionService.calculateOrderDiscount(
                new BigDecimal("50.00"), rules
        );
        
        // Then: 折扣应该等于订单金额
        assertEquals(new BigDecimal("50.00"), discount);
    }
    
    /**
     * 测试：固定金额+折扣率组合
     */
    @Test
    public void testCombinedDiscount_ShouldBeDeterministic() {
        // Given: 固定减5元 + 9折
        CampaignRulesDTO rules = CampaignRulesDTO.builder()
                .discountAmount(new BigDecimal("5.00"))
                .discountRate(new BigDecimal("0.90"))
                .build();
        
        BigDecimal orderAmount = new BigDecimal("100.00");
        
        // When: 计算折扣
        BigDecimal discount1 = executionService.calculateOrderDiscount(orderAmount, rules);
        BigDecimal discount2 = executionService.calculateOrderDiscount(orderAmount, rules);
        
        // Then: 应该一致（5 + 10 = 15元）
        assertEquals(new BigDecimal("15.00"), discount1);
        assertEquals(discount1, discount2);
    }
    
    /**
     * 测试：零金额订单
     */
    @Test
    public void testZeroOrderAmount_ShouldReturnZeroDiscount() {
        // Given
        CampaignRulesDTO rules = CampaignRulesDTO.builder()
                .discountAmount(new BigDecimal("10.00"))
                .build();
        
        // When: 订单金额为0
        BigDecimal discount = executionService.calculateOrderDiscount(
                BigDecimal.ZERO, rules
        );
        
        // Then: 折扣为0
        assertEquals(BigDecimal.ZERO, discount);
    }
}
