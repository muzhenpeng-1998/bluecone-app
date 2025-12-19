package com.bluecone.app.wallet.infra.persistence.repository;

import com.bluecone.app.wallet.domain.model.WalletLedger;
import com.bluecone.app.wallet.domain.repository.WalletLedgerRepository;
import com.bluecone.app.wallet.infra.persistence.converter.WalletConverter;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletLedgerMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletLedgerPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 钱包账本流水仓储实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Repository
@RequiredArgsConstructor
public class WalletLedgerRepositoryImpl implements WalletLedgerRepository {
    
    private final WalletLedgerMapper ledgerMapper;
    
    @Override
    public Optional<WalletLedger> findByIdemKey(Long tenantId, String idemKey) {
        WalletLedgerPO po = ledgerMapper.selectByIdemKey(tenantId, idemKey);
        return Optional.ofNullable(WalletConverter.toLedgerDomain(po));
    }
    
    @Override
    public void insert(WalletLedger ledger) {
        WalletLedgerPO po = WalletConverter.toLedgerPO(ledger);
        ledgerMapper.insert(po);
    }
    
    @Override
    public List<WalletLedger> findByUserId(Long tenantId, Long userId, int offset, int limit) {
        List<WalletLedgerPO> poList = ledgerMapper.selectByUserId(tenantId, userId, offset, limit);
        return poList.stream()
                .map(WalletConverter::toLedgerDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WalletLedger> findByAccountId(Long tenantId, Long accountId, int offset, int limit) {
        List<WalletLedgerPO> poList = ledgerMapper.selectByAccountId(tenantId, accountId, offset, limit);
        return poList.stream()
                .map(WalletConverter::toLedgerDomain)
                .collect(Collectors.toList());
    }
}
