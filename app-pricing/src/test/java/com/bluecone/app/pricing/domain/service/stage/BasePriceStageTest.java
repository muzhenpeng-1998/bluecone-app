package com.bluecone.app.pricing.domain.service.stage;

import com.bluecone.app.pricing.api.dto.PricingItem;
import com.bluecone.app.pricing.api.dto.PricingRequest;
import com.bluecone.app.pricing.api.enums.ReasonCode;
import com.bluecone.app.pricing.domain.model.PricingContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基价计算阶段测试
 */
@DisplayName("基价计算阶段测试")
class BasePriceStageTest {
    
    private final BasePriceStage stage = new BasePriceStage();
    
    @Test
    @DisplayName("测试基价计算：单商品无规格加价")
    void testSingleItemWithoutSurcharge() {
        // Given
        PricingRequest request = new PricingRequest();
        List<PricingItem> items = new ArrayList<>();
        
        PricingItem item = new PricingItem();
        item.setSkuId(1001L);
        item.setSkuName("商品A");
        item.setQuantity(3);
        item.setBasePrice(new BigDecimal("10.00"));
        item.setSpecSurcharge(BigDecimal.ZERO);
        items.add(item);
        
        request.setItems(items);
        PricingContext context = new PricingContext(request);
        
        // When
        stage.execute(context);
        
        // Then
        assertThat(context.getOriginalAmount()).isEqualByComparingTo("30.00");
        assertThat(context.getCurrentAmount()).isEqualByComparingTo("30.00");
        assertThat(context.getBreakdownLines()).hasSize(1);
        assertThat(context.getBreakdownLines().get(0).getReasonCode()).isEqualTo(ReasonCode.BASE_PRICE);
    }
    
    @Test
    @DisplayName("测试基价计算：单商品有规格加价")
    void testSingleItemWithSurcharge() {
        // Given
        PricingRequest request = new PricingRequest();
        List<PricingItem> items = new ArrayList<>();
        
        PricingItem item = new PricingItem();
        item.setSkuId(1001L);
        item.setSkuName("商品A");
        item.setQuantity(2);
        item.setBasePrice(new BigDecimal("10.00"));
        item.setSpecSurcharge(new BigDecimal("2.00")); // 规格加价2元
        items.add(item);
        
        request.setItems(items);
        PricingContext context = new PricingContext(request);
        
        // When
        stage.execute(context);
        
        // Then
        assertThat(context.getOriginalAmount()).isEqualByComparingTo("24.00"); // (10+2)*2
        assertThat(context.getCurrentAmount()).isEqualByComparingTo("24.00");
        assertThat(context.getBreakdownLines()).hasSize(2); // 基价 + 规格加价
    }
    
    @Test
    @DisplayName("测试基价计算：多商品")
    void testMultipleItems() {
        // Given
        PricingRequest request = new PricingRequest();
        List<PricingItem> items = new ArrayList<>();
        
        PricingItem item1 = new PricingItem();
        item1.setSkuId(1001L);
        item1.setSkuName("商品A");
        item1.setQuantity(2);
        item1.setBasePrice(new BigDecimal("10.00"));
        item1.setSpecSurcharge(BigDecimal.ZERO);
        items.add(item1);
        
        PricingItem item2 = new PricingItem();
        item2.setSkuId(1002L);
        item2.setSkuName("商品B");
        item2.setQuantity(1);
        item2.setBasePrice(new BigDecimal("30.00"));
        item2.setSpecSurcharge(new BigDecimal("5.00"));
        items.add(item2);
        
        request.setItems(items);
        PricingContext context = new PricingContext(request);
        
        // When
        stage.execute(context);
        
        // Then
        assertThat(context.getOriginalAmount()).isEqualByComparingTo("55.00"); // 10*2 + 30 + 5
        assertThat(context.getCurrentAmount()).isEqualByComparingTo("55.00");
        assertThat(context.getBreakdownLines()).hasSize(3); // 商品A基价 + 商品B基价 + 商品B规格加价
    }
}
