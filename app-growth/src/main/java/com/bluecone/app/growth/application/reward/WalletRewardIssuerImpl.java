package com.bluecone.app.growth.application.reward;

import com.bluecone.app.growth.domain.service.RewardIssuanceService.WalletRewardIssuer;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;
import com.bluecone.app.wallet.api.facade.WalletAssetFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 储值奖励发放器实现
 * 使用 WalletAssetFacade 进行钱包入账操作
 */
@Slf4j
@Component
public class WalletRewardIssuerImpl implements WalletRewardIssuer {
    
    private final IdService idService;
    private final ObjectMapper objectMapper;
    private final WalletAssetFacade walletAssetFacade;

    public WalletRewardIssuerImpl(IdService idService,
                                 @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
                                 WalletAssetFacade walletAssetFacade) {
        this.idService = idService;
        this.objectMapper = objectMapper;
        this.walletAssetFacade = walletAssetFacade;
    }
    
    @Override
    public String issue(Long tenantId, Long userId, String amountStr, String idempotencyKey) {
        try {
            // 解析金额
            Map<String, Object> valueMap = objectMapper.readValue(amountStr, Map.class);
            Long amountInCents = Long.valueOf(valueMap.get("amount").toString());
            BigDecimal amount = new BigDecimal(amountInCents).divide(new BigDecimal(100));
            
            // 调用钱包资产门面，直接入账（commit操作，不需要冻结）
            WalletAssetCommand command = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(amount)
                    .bizType("GROWTH_REWARD")
                    .bizId(idempotencyKey)
                    .idempotencyKey(idempotencyKey)
                    .remark("邀新活动奖励")
                    .build();
            
            // 直接提交入账（增长活动奖励不需要冻结，直接到账）
            WalletAssetResult result = walletAssetFacade.commit(command);
            
            return String.valueOf(result.getLedgerId());
            
        } catch (Exception e) {
            log.error("储值奖励发放失败: tenantId={}, userId={}, amountStr={}", 
                    tenantId, userId, amountStr, e);
            throw new RuntimeException("储值发放失败: " + e.getMessage(), e);
        }
    }
}
