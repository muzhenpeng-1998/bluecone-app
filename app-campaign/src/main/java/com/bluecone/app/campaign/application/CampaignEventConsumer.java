package com.bluecone.app.campaign.application;

import com.bluecone.app.campaign.api.dto.CampaignQueryContext;
import com.bluecone.app.campaign.api.enums.CampaignType;
import com.bluecone.app.campaign.domain.model.Campaign;
import com.bluecone.app.campaign.domain.service.CampaignExecutionService;
import com.bluecone.app.campaign.domain.service.CampaignQueryService;
import com.bluecone.app.core.event.outbox.EventType;
import com.bluecone.app.infra.event.outbox.OutboxEventDO;
import com.bluecone.app.infra.event.outbox.handler.OutboxEventHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 活动事件消费者
 * 消费 ORDER_PAID 和 RECHARGE_PAID 事件，执行异步活动
 */
@Slf4j
@Component
public class CampaignEventConsumer implements OutboxEventHandler {
    
    private final CampaignQueryService campaignQueryService;
    private final CampaignExecutionService campaignExecutionService;
    private final ObjectMapper objectMapper;

    public CampaignEventConsumer(CampaignQueryService campaignQueryService,
                                 CampaignExecutionService campaignExecutionService,
                                 @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.campaignQueryService = campaignQueryService;
        this.campaignExecutionService = campaignExecutionService;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public boolean supports(OutboxEventDO event) {
        return EventType.ORDER_PAID.getCode().equals(event.getEventType()) 
                || EventType.RECHARGE_PAID.getCode().equals(event.getEventType());
    }
    
    @Override
    public void handle(OutboxEventDO event) throws Exception {
        String eventType = event.getEventType();
        
        log.info("[campaign-consumer] 消费事件，eventId={}, eventType={}", event.getId(), eventType);
        
        if (EventType.ORDER_PAID.getCode().equals(eventType)) {
            handleOrderPaid(event);
        } else if (EventType.RECHARGE_PAID.getCode().equals(eventType)) {
            handleRechargePaid(event);
        }
    }
    
    /**
     * 处理订单支付成功事件
     */
    private void handleOrderPaid(OutboxEventDO event) throws Exception {
        try {
            // 解析事件载荷
            Map<String, Object> payload = objectMapper.readValue(event.getEventBody(), Map.class);
            Long tenantId = getLongValue(payload, "tenantId");
            Long userId = getLongValue(payload, "userId");
            Long orderId = getLongValue(payload, "orderId");
            String orderNo = (String) payload.get("orderNo");
            BigDecimal orderAmount = getBigDecimalValue(payload, "orderAmount");
            Long storeId = getLongValue(payload, "storeId");
            
            if (tenantId == null || userId == null || orderId == null || orderAmount == null) {
                log.error("[campaign-consumer] ORDER_PAID 事件载荷缺少必要字段，eventId={}, payload={}", 
                        event.getId(), payload);
                return;
            }
            
            // 查询可用的返券活动
            CampaignQueryContext context = CampaignQueryContext.builder()
                    .tenantId(tenantId)
                    .campaignType(CampaignType.ORDER_REBATE_COUPON)
                    .storeId(storeId)
                    .userId(userId)
                    .amount(orderAmount)
                    .build();
            
            List<Campaign> campaigns = campaignQueryService.queryAvailableCampaigns(context);
            
            log.info("[campaign-consumer] ORDER_PAID 匹配到 {} 个返券活动，orderId={}", campaigns.size(), orderId);
            
            // 执行每个活动
            for (Campaign campaign : campaigns) {
                try {
                    campaignExecutionService.executeCampaign(
                            campaign, tenantId, userId, orderId, orderNo, orderAmount
                    );
                } catch (Exception e) {
                    log.error("[campaign-consumer] 执行返券活动失败，campaign={}, orderId={}", 
                            campaign.getCampaignCode(), orderId, e);
                    // 继续执行其他活动，不抛异常
                }
            }
            
        } catch (Exception e) {
            log.error("[campaign-consumer] 处理 ORDER_PAID 事件失败，eventId={}", event.getId(), e);
            throw e;
        }
    }
    
    /**
     * 处理充值支付成功事件
     */
    private void handleRechargePaid(OutboxEventDO event) throws Exception {
        try {
            // 解析事件载荷
            Map<String, Object> payload = objectMapper.readValue(event.getEventBody(), Map.class);
            Long tenantId = getLongValue(payload, "tenantId");
            Long userId = getLongValue(payload, "userId");
            Long rechargeId = getLongValue(payload, "rechargeId");
            String rechargeNo = (String) payload.get("rechargeNo");
            BigDecimal rechargeAmount = getBigDecimalValue(payload, "rechargeAmount");
            
            if (tenantId == null || userId == null || rechargeId == null || rechargeAmount == null) {
                log.error("[campaign-consumer] RECHARGE_PAID 事件载荷缺少必要字段，eventId={}, payload={}", 
                        event.getId(), payload);
                return;
            }
            
            // 查询可用的充值赠送活动
            CampaignQueryContext context = CampaignQueryContext.builder()
                    .tenantId(tenantId)
                    .campaignType(CampaignType.RECHARGE_BONUS)
                    .userId(userId)
                    .amount(rechargeAmount)
                    .build();
            
            List<Campaign> campaigns = campaignQueryService.queryAvailableCampaigns(context);
            
            log.info("[campaign-consumer] RECHARGE_PAID 匹配到 {} 个充值赠送活动，rechargeId={}", 
                    campaigns.size(), rechargeId);
            
            // 执行每个活动
            for (Campaign campaign : campaigns) {
                try {
                    campaignExecutionService.executeCampaign(
                            campaign, tenantId, userId, rechargeId, rechargeNo, rechargeAmount
                    );
                } catch (Exception e) {
                    log.error("[campaign-consumer] 执行充值赠送活动失败，campaign={}, rechargeId={}", 
                            campaign.getCampaignCode(), rechargeId, e);
                    // 继续执行其他活动，不抛异常
                }
            }
            
        } catch (Exception e) {
            log.error("[campaign-consumer] 处理 RECHARGE_PAID 事件失败，eventId={}", event.getId(), e);
            throw e;
        }
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
