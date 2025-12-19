package com.bluecone.app.member.infra.repository;

import com.bluecone.app.member.domain.model.PointsAccount;
import com.bluecone.app.member.domain.repository.PointsAccountRepository;
import com.bluecone.app.member.infra.converter.PointsAccountConverter;
import com.bluecone.app.member.infra.persistence.mapper.PointsAccountMapper;
import com.bluecone.app.member.infra.persistence.po.PointsAccountPO;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 积分账户仓储实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Repository("memberPointsAccountRepositoryImpl")
public class PointsAccountRepositoryImpl implements PointsAccountRepository {
    
    private final PointsAccountMapper accountMapper;
    
    public PointsAccountRepositoryImpl(PointsAccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }
    
    @Override
    public Optional<PointsAccount> findByMemberId(Long tenantId, Long memberId) {
        PointsAccountPO po = accountMapper.selectByMemberId(tenantId, memberId);
        return Optional.ofNullable(PointsAccountConverter.toDomain(po));
    }
    
    @Override
    public void save(PointsAccount account) {
        PointsAccountPO po = PointsAccountConverter.toPO(account);
        accountMapper.insert(po);
    }
    
    @Override
    public boolean updateWithVersion(PointsAccount account) {
        PointsAccountPO po = PointsAccountConverter.toPO(account);
        int updated = accountMapper.updateWithVersion(po);
        if (updated > 0) {
            // 更新成功后，版本号需要递增（模拟数据库的 version + 1）
            account.setVersion(account.getVersion() + 1);
            return true;
        }
        return false;
    }
}
