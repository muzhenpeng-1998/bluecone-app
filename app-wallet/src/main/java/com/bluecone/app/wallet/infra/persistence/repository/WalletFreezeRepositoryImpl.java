package com.bluecone.app.wallet.infra.persistence.repository;

import com.bluecone.app.wallet.domain.model.WalletFreeze;
import com.bluecone.app.wallet.domain.repository.WalletFreezeRepository;
import com.bluecone.app.wallet.infra.persistence.converter.WalletConverter;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletFreezeMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletFreezePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钱包冻结记录仓储实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Repository
@RequiredArgsConstructor
public class WalletFreezeRepositoryImpl implements WalletFreezeRepository {
    
    private final WalletFreezeMapper freezeMapper;
    
    @Override
    public WalletFreeze findByIdemKey(Long tenantId, String idemKey) {
        WalletFreezePO po = freezeMapper.selectByIdemKey(tenantId, idemKey);
        return WalletConverter.toFreezeDomain(po);
    }
    
    @Override
    public WalletFreeze findByBizOrderId(Long tenantId, String bizType, Long bizOrderId) {
        WalletFreezePO po = freezeMapper.selectByBizOrderId(tenantId, bizType, bizOrderId);
        return WalletConverter.toFreezeDomain(po);
    }
    
    @Override
    public void insert(WalletFreeze freeze) {
        WalletFreezePO po = WalletConverter.toFreezePO(freeze);
        freezeMapper.insert(po);
    }
    
    @Override
    public int updateWithVersion(WalletFreeze freeze) {
        WalletFreezePO po = WalletConverter.toFreezePO(freeze);
        return freezeMapper.updateWithVersion(po);
    }
    
    @Override
    public List<WalletFreeze> findExpiredFreezes(LocalDateTime now, int limit) {
        List<WalletFreezePO> poList = freezeMapper.selectExpiredFreezes(now, limit);
        return poList.stream()
                .map(WalletConverter::toFreezeDomain)
                .collect(Collectors.toList());
    }
}
