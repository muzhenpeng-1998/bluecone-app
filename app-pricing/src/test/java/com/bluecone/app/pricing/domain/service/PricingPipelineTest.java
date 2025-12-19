package com.bluecone.app.pricing.domain.service;

import com.bluecone.app.member.api.dto.PointsBalanceDTO;
import com.bluecone.app.member.api.facade.MemberQueryFacade;
import com.bluecone.app.pricing.api.dto.*;
import com.bluecone.app.pricing.api.enums.ReasonCode;
import com.bluecone.app.pricing.domain.service.stage.*;
import com.bluecone.app.promo.api.dto.CouponQueryContext;
import com.bluecone.app.promo.api.dto.UsableCouponDTO;
import com.bluecone.app.promo.api.facade.CouponQueryFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 计价流水线测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("计价流水线测试")
class PricingPipelineTest {
    
    @Mock
    private CouponQueryFacade couponQueryFacade;
    
    @Mock
    private MemberQueryFacade memberQueryFacade;
    
    private PricingPipeline pricingPipeline;
    
    @BeforeEach
    void setUp() {
        // 创建所有 Stage
        List<PricingStage> stages = new ArrayList<>();
        stages.add(new BasePriceStage());
        stages.add(new MemberPriceStage());
        stages.add(new PromoStage());
        stages.add(new CouponStage(couponQueryFacade));
        stages.add(new PointsStage(memberQueryFacade));
        stages.add(new FeeStage());
        stages.add(new RoundingStage());
        
        pricingPipeline = new PricingPipeline(stages);
    }
    
    @Test
    @DisplayName("测试基础计价：只有商品基价，无优惠")
    void testBasicPricing() {
        // Given
        PricingRequest request = createBasicRequest();
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then
        assertThat(quote).isNotNull();
        assertThat(quote.getQuoteId()).isNotBlank();
        assertThat(quote.getPricingVersion()).isEqualTo("1.0.0");
        assertThat(quote.getOriginalAmount()).isEqualByComparingTo("50.00"); // 10*2 + 30*1
        assertThat(quote.getPayableAmount()).isEqualByComparingTo("56.00"); // 50 + 5(配送费) + 1(打包费)
        assertThat(quote.getBreakdownLines()).hasSize(4); // 2商品 + 配送费 + 打包费
    }
    
    @Test
    @DisplayName("测试固定输入输出：相同输入应产生相同输出")
    void testDeterministicPricing() {
        // Given
        PricingRequest request = createBasicRequest();
        
        // When
        PricingQuote quote1 = pricingPipeline.execute(request);
        PricingQuote quote2 = pricingPipeline.execute(request);
        
        // Then - 金额应该完全一致
        assertThat(quote1.getOriginalAmount()).isEqualByComparingTo(quote2.getOriginalAmount());
        assertThat(quote1.getPayableAmount()).isEqualByComparingTo(quote2.getPayableAmount());
        assertThat(quote1.getBreakdownLines()).hasSameSizeAs(quote2.getBreakdownLines());
    }
    
    @Test
    @DisplayName("测试优惠券抵扣")
    void testCouponDiscount() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setCouponId(1001L);
        
        // Mock 优惠券查询
        UsableCouponDTO coupon = new UsableCouponDTO();
        coupon.setCouponId(1001L);
        coupon.setUsable(true);
        coupon.setEstimatedDiscount(new BigDecimal("10.00"));
        coupon.setDescription("满50减10");
        
        when(couponQueryFacade.listUsableCoupons(any(CouponQueryContext.class)))
                .thenReturn(List.of(coupon));
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then
        assertThat(quote.getCouponDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(quote.getAppliedCouponId()).isEqualTo(1001L);
        assertThat(quote.getPayableAmount()).isEqualByComparingTo("46.00"); // 50 - 10 + 5 + 1
        
        // 验证明细行包含优惠券
        assertThat(quote.getBreakdownLines())
                .anyMatch(line -> line.getReasonCode() == ReasonCode.COUPON_DISCOUNT);
    }
    
    @Test
    @DisplayName("测试积分抵扣")
    void testPointsDiscount() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setMemberId(2001L);
        request.setUsePoints(500); // 使用500积分
        
        // Mock 积分余额查询
        PointsBalanceDTO balance = new PointsBalanceDTO();
        balance.setAvailablePoints(1000);
        
        when(memberQueryFacade.getPointsBalance(any(), any()))
                .thenReturn(balance);
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then
        assertThat(quote.getPointsDiscountAmount()).isEqualByComparingTo("5.00"); // 500 * 0.01
        assertThat(quote.getAppliedPoints()).isEqualTo(500);
        assertThat(quote.getPayableAmount()).isEqualByComparingTo("51.00"); // 50 - 5 + 5 + 1
        
        // 验证明细行包含积分抵扣
        assertThat(quote.getBreakdownLines())
                .anyMatch(line -> line.getReasonCode() == ReasonCode.POINTS_DISCOUNT);
    }
    
    @Test
    @DisplayName("测试积分抵扣上限：不能超过订单金额的50%")
    void testPointsDiscountLimit() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setMemberId(2001L);
        request.setUsePoints(10000); // 使用10000积分，理论上可抵扣100元
        
        // Mock 积分余额查询
        PointsBalanceDTO balance = new PointsBalanceDTO();
        balance.setAvailablePoints(10000);
        
        when(memberQueryFacade.getPointsBalance(any(), any()))
                .thenReturn(balance);
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then - 应该被限制在订单金额的50%
        assertThat(quote.getPointsDiscountAmount()).isEqualByComparingTo("25.00"); // 50 * 0.5
        assertThat(quote.getAppliedPoints()).isEqualTo(2500); // 25 / 0.01
    }
    
    @Test
    @DisplayName("测试配送费计算：3公里内起步价")
    void testDeliveryFeeWithinBaseDistance() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setDeliveryMode("DELIVERY");
        request.setDeliveryDistance(new BigDecimal("2.5"));
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then
        assertThat(quote.getDeliveryFee()).isEqualByComparingTo("5.00"); // 起步价
    }
    
    @Test
    @DisplayName("测试配送费计算：超过3公里按距离加价")
    void testDeliveryFeeBeyondBaseDistance() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setDeliveryMode("DELIVERY");
        request.setDeliveryDistance(new BigDecimal("5.5"));
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then
        // 5 + (5.5 - 3) * 2 = 5 + 5 = 10
        assertThat(quote.getDeliveryFee()).isEqualByComparingTo("10.00");
    }
    
    @Test
    @DisplayName("测试抹零：四舍五入到角")
    void testRounding() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setEnableRounding(true);
        
        // 修改价格使其产生抹零
        request.getItems().get(0).setBasePrice(new BigDecimal("10.13"));
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then - 应该抹零到角
        String payableStr = quote.getPayableAmount().toPlainString();
        assertThat(payableStr).matches("\\d+\\.\\d"); // 只有一位小数
    }
    
    @Test
    @DisplayName("测试边界情况：优惠券不可用")
    void testCouponNotUsable() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setCouponId(1001L);
        
        // Mock 优惠券不可用
        UsableCouponDTO coupon = new UsableCouponDTO();
        coupon.setCouponId(1001L);
        coupon.setUsable(false);
        coupon.setUnusableReason("订单金额不满足最低使用条件");
        
        when(couponQueryFacade.listUsableCoupons(any(CouponQueryContext.class)))
                .thenReturn(List.of(coupon));
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then - 不应该有优惠券抵扣
        assertThat(quote.getCouponDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(quote.getAppliedCouponId()).isNull();
    }
    
    @Test
    @DisplayName("测试边界情况：积分余额不足")
    void testInsufficientPoints() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setMemberId(2001L);
        request.setUsePoints(1000);
        
        // Mock 积分余额不足
        PointsBalanceDTO balance = new PointsBalanceDTO();
        balance.setAvailablePoints(500);
        
        when(memberQueryFacade.getPointsBalance(any(), any()))
                .thenReturn(balance);
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then - 不应该有积分抵扣
        assertThat(quote.getPointsDiscountAmount()).isEqualByComparingTo("0.00");
        assertThat(quote.getAppliedPoints()).isNull();
    }
    
    @Test
    @DisplayName("测试组合优惠：优惠券+积分")
    void testCombinedDiscounts() {
        // Given
        PricingRequest request = createBasicRequest();
        request.setCouponId(1001L);
        request.setMemberId(2001L);
        request.setUsePoints(500);
        
        // Mock 优惠券
        UsableCouponDTO coupon = new UsableCouponDTO();
        coupon.setCouponId(1001L);
        coupon.setUsable(true);
        coupon.setEstimatedDiscount(new BigDecimal("10.00"));
        coupon.setDescription("满50减10");
        
        when(couponQueryFacade.listUsableCoupons(any(CouponQueryContext.class)))
                .thenReturn(List.of(coupon));
        
        // Mock 积分余额
        PointsBalanceDTO balance = new PointsBalanceDTO();
        balance.setAvailablePoints(1000);
        
        when(memberQueryFacade.getPointsBalance(any(), any()))
                .thenReturn(balance);
        
        // When
        PricingQuote quote = pricingPipeline.execute(request);
        
        // Then
        assertThat(quote.getCouponDiscountAmount()).isEqualByComparingTo("10.00");
        assertThat(quote.getPointsDiscountAmount()).isEqualByComparingTo("5.00");
        assertThat(quote.getPayableAmount()).isEqualByComparingTo("41.00"); // 50 - 10 - 5 + 5 + 1
    }
    
    /**
     * 创建基础测试请求
     */
    private PricingRequest createBasicRequest() {
        PricingRequest request = new PricingRequest();
        request.setTenantId(1L);
        request.setStoreId(100L);
        request.setUserId(1000L);
        request.setDeliveryMode("DELIVERY");
        request.setDeliveryDistance(new BigDecimal("2.0"));
        
        List<PricingItem> items = new ArrayList<>();
        
        // 商品1：单价10元，数量2
        PricingItem item1 = new PricingItem();
        item1.setSkuId(1001L);
        item1.setSkuName("商品A");
        item1.setQuantity(2);
        item1.setBasePrice(new BigDecimal("10.00"));
        item1.setSpecSurcharge(BigDecimal.ZERO);
        items.add(item1);
        
        // 商品2：单价30元，数量1
        PricingItem item2 = new PricingItem();
        item2.setSkuId(1002L);
        item2.setSkuName("商品B");
        item2.setQuantity(1);
        item2.setBasePrice(new BigDecimal("30.00"));
        item2.setSpecSurcharge(BigDecimal.ZERO);
        items.add(item2);
        
        request.setItems(items);
        
        return request;
    }
}
