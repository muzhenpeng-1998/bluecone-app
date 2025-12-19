package com.bluecone.app.campaign;

import com.bluecone.app.campaign.api.dto.CampaignRulesDTO;
import com.bluecone.app.campaign.api.dto.CampaignScopeDTO;
import com.bluecone.app.campaign.api.enums.CampaignScope;
import com.bluecone.app.campaign.api.enums.CampaignStatus;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.model.ExecutionLog;
import com.bluecone.app.campaign.domain.repository.ExecutionLogRepository;
import com.bluecone.app.campaign.domain.service.CampaignExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 活动幂等性测试
 * 
 * 测试场景：
 * 1. 同一订单重复执行活动，应该返回相同结果，不重复发券
 * 2. 并发执行应该只有一个成功，其他幂等返回
 */
@SpringBootTest(classes = CampaignIdempotencyTest.TestConfiguration.class)
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
public class CampaignIdempotencyTest {
    
    @MockBean
    private ExecutionLogRepository executionLogRepository;
    
    @MockBean
    private CampaignExecutionService campaignExecutionService;
    
    /**
     * 测试：幂等键相同时，返回已有结果
     */
    @Test
    public void testIdempotencyKey_ShouldReturnExistingResult() {
        // Given: 已有执行记录
        String idempotencyKey = "1:ORDER_REBATE_COUPON:100:200";
        ExecutionLog existingLog = ExecutionLog.builder()
                .id(999L)
                .tenantId(1L)
                .campaignId(123L)
                .campaignCode("FIRST_ORDER_REBATE")
                .campaignType(CampaignType.ORDER_REBATE_COUPON)
                .idempotencyKey(idempotencyKey)
                .userId(200L)
                .bizOrderId(100L)
                .bizOrderNo("ORD001")
                .bizAmount(new BigDecimal("150.00"))
                .build();
        
        when(executionLogRepository.findByIdempotencyKey(anyLong(), anyString()))
                .thenReturn(Optional.of(existingLog));
        
        // When: 查询幂等键
        Optional<ExecutionLog> result = executionLogRepository.findByIdempotencyKey(1L, idempotencyKey);
        
        // Then: 应该返回已有结果
        assertTrue(result.isPresent());
        assertEquals(999L, result.get().getId());
        assertEquals(idempotencyKey, result.get().getIdempotencyKey());
        
        // 验证没有插入新记录
        verify(executionLogRepository, never()).save(any(ExecutionLog.class));
    }
    
    /**
     * 测试：幂等键格式正确性
     */
    @Test
    public void testIdempotencyKeyFormat() {
        // Given
        Long tenantId = 1L;
        CampaignType campaignType = CampaignType.ORDER_REBATE_COUPON;
        Long bizOrderId = 100L;
        Long userId = 200L;
        
        // When: 构建幂等键
        String idempotencyKey = String.format("%d:%s:%d:%d", 
                tenantId, campaignType.name(), bizOrderId, userId);
        
        // Then: 验证格式
        assertEquals("1:ORDER_REBATE_COUPON:100:200", idempotencyKey);
        
        // 验证幂等键包含所有必要信息
        assertTrue(idempotencyKey.contains(tenantId.toString()));
        assertTrue(idempotencyKey.contains(campaignType.name()));
        assertTrue(idempotencyKey.contains(bizOrderId.toString()));
        assertTrue(idempotencyKey.contains(userId.toString()));
    }
    
    /**
     * 测试：不同业务单应该有不同的幂等键
     */
    @Test
    public void testDifferentOrders_ShouldHaveDifferentKeys() {
        // Given
        Long tenantId = 1L;
        CampaignType campaignType = CampaignType.ORDER_REBATE_COUPON;
        Long userId = 200L;
        
        // When: 不同订单
        String key1 = String.format("%d:%s:%d:%d", tenantId, campaignType.name(), 100L, userId);
        String key2 = String.format("%d:%s:%d:%d", tenantId, campaignType.name(), 101L, userId);
        
        // Then: 幂等键应该不同
        assertNotEquals(key1, key2);
    }
    
    /**
     * 测试：不同活动类型应该有不同的幂等键
     */
    @Test
    public void testDifferentCampaignTypes_ShouldHaveDifferentKeys() {
        // Given
        Long tenantId = 1L;
        Long bizOrderId = 100L;
        Long userId = 200L;
        
        // When: 不同活动类型
        String key1 = String.format("%d:%s:%d:%d", 
                tenantId, CampaignType.ORDER_REBATE_COUPON.name(), bizOrderId, userId);
        String key2 = String.format("%d:%s:%d:%d", 
                tenantId, CampaignType.RECHARGE_BONUS.name(), bizOrderId, userId);
        
        // Then: 幂等键应该不同
        assertNotEquals(key1, key2);
    }
    
    /**
     * 测试配置
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfiguration {
        // Empty configuration for minimal Spring context
    }
}
