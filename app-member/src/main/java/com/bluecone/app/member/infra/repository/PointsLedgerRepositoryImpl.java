package com.bluecone.app.member.infra.repository;

import com.bluecone.app.member.domain.model.PointsLedger;
import com.bluecone.app.member.domain.repository.PointsLedgerRepository;
import com.bluecone.app.member.infra.converter.PointsLedgerConverter;
import com.bluecone.app.member.infra.persistence.mapper.PointsLedgerMapper;
import com.bluecone.app.member.infra.persistence.po.PointsLedgerPO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 积分流水仓储实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Repository("memberPointsLedgerRepositoryImpl")
public class PointsLedgerRepositoryImpl implements PointsLedgerRepository {
    
    private static final Logger log = LoggerFactory.getLogger(PointsLedgerRepositoryImpl.class);
    
    private final PointsLedgerMapper ledgerMapper;
    
    public PointsLedgerRepositoryImpl(PointsLedgerMapper ledgerMapper) {
        this.ledgerMapper = ledgerMapper;
    }
    
    @Override
    public Optional<PointsLedger> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        PointsLedgerPO po = ledgerMapper.selectByIdempotencyKey(tenantId, idempotencyKey);
        return Optional.ofNullable(PointsLedgerConverter.toDomain(po));
    }
    
    @Override
    public boolean save(PointsLedger ledger) {
        try {
            PointsLedgerPO po = PointsLedgerConverter.toPO(ledger);
            ledgerMapper.insert(po);
            return true;
        } catch (DuplicateKeyException e) {
            // 幂等键唯一约束冲突，说明已经存在相同的流水记录
            log.warn("积分流水幂等键冲突，幂等键：{}，错误：{}", ledger.getIdempotencyKey(), e.getMessage());
            return false;
        }
    }
}
