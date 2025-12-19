package com.bluecone.app.wallet.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.wallet.domain.model.RechargeOrder;
import com.bluecone.app.wallet.domain.repository.RechargeOrderRepository;
import com.bluecone.app.wallet.infra.persistence.converter.WalletConverter;
import com.bluecone.app.wallet.infra.persistence.mapper.RechargeOrderMapper;
import com.bluecone.app.wallet.infra.persistence.po.RechargeOrderPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 充值单仓储实现
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RechargeOrderRepositoryImpl implements RechargeOrderRepository {
    
    private final RechargeOrderMapper rechargeOrderMapper;
    
    @Override
    public RechargeOrder save(RechargeOrder rechargeOrder) {
        RechargeOrderPO po = WalletConverter.toRechargeOrderPO(rechargeOrder);
        rechargeOrderMapper.insert(po);
        return WalletConverter.toRechargeOrderDomain(po);
    }
    
    @Override
    public Optional<RechargeOrder> findById(Long tenantId, Long id) {
        LambdaQueryWrapper<RechargeOrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrderPO::getTenantId, tenantId)
               .eq(RechargeOrderPO::getId, id);
        RechargeOrderPO po = rechargeOrderMapper.selectOne(wrapper);
        return Optional.ofNullable(WalletConverter.toRechargeOrderDomain(po));
    }
    
    @Override
    public Optional<RechargeOrder> findByRechargeNo(Long tenantId, String rechargeNo) {
        LambdaQueryWrapper<RechargeOrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrderPO::getTenantId, tenantId)
               .eq(RechargeOrderPO::getRechargeNo, rechargeNo);
        RechargeOrderPO po = rechargeOrderMapper.selectOne(wrapper);
        return Optional.ofNullable(WalletConverter.toRechargeOrderDomain(po));
    }
    
    @Override
    public Optional<RechargeOrder> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        LambdaQueryWrapper<RechargeOrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrderPO::getTenantId, tenantId)
               .eq(RechargeOrderPO::getIdemKey, idempotencyKey);
        RechargeOrderPO po = rechargeOrderMapper.selectOne(wrapper);
        return Optional.ofNullable(WalletConverter.toRechargeOrderDomain(po));
    }
    
    @Override
    public Optional<RechargeOrder> findByChannelTradeNo(Long tenantId, String channelTradeNo) {
        if (channelTradeNo == null) {
            return Optional.empty();
        }
        LambdaQueryWrapper<RechargeOrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrderPO::getTenantId, tenantId)
               .eq(RechargeOrderPO::getPayNo, channelTradeNo);
        RechargeOrderPO po = rechargeOrderMapper.selectOne(wrapper);
        return Optional.ofNullable(WalletConverter.toRechargeOrderDomain(po));
    }
    
    @Override
    public int updateWithVersion(RechargeOrder rechargeOrder) {
        RechargeOrderPO po = WalletConverter.toRechargeOrderPO(rechargeOrder);
        return rechargeOrderMapper.updateWithVersion(po);
    }
}
