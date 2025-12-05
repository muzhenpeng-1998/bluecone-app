package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.user.domain.account.BalanceAccount;
import com.bluecone.app.core.user.domain.repository.BalanceAccountRepository;
import com.bluecone.app.infra.user.dataobject.MemberBalanceAccountDO;
import com.bluecone.app.infra.user.mapper.MemberBalanceAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 储值账户仓储实现，基于表 bc_member_balance_account。
 */
@Repository
@RequiredArgsConstructor
public class BalanceAccountRepositoryImpl implements BalanceAccountRepository {

    private final MemberBalanceAccountMapper memberBalanceAccountMapper;

    @Override
    public Optional<BalanceAccount> findByTenantAndMember(Long tenantId, Long memberId) {
        MemberBalanceAccountDO dataObject = memberBalanceAccountMapper.selectOne(new LambdaQueryWrapper<MemberBalanceAccountDO>()
                .eq(MemberBalanceAccountDO::getTenantId, tenantId)
                .eq(MemberBalanceAccountDO::getMemberId, memberId));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public BalanceAccount save(BalanceAccount account) {
        MemberBalanceAccountDO dataObject = toDO(account);
        if (account.getId() == null) {
            memberBalanceAccountMapper.insert(dataObject);
            account.setId(dataObject.getId());
        } else {
            memberBalanceAccountMapper.updateById(dataObject);
        }
        return account;
    }

    @Override
    public boolean saveWithVersion(BalanceAccount account, long expectedVersion) {
        MemberBalanceAccountDO dataObject = toDO(account);
        LambdaUpdateWrapper<MemberBalanceAccountDO> wrapper = new LambdaUpdateWrapper<MemberBalanceAccountDO>()
                .eq(MemberBalanceAccountDO::getId, account.getId())
                .eq(MemberBalanceAccountDO::getVersion, expectedVersion);
        return memberBalanceAccountMapper.update(dataObject, wrapper) > 0;
    }

    private BalanceAccount toDomain(MemberBalanceAccountDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        BalanceAccount account = new BalanceAccount();
        account.setId(dataObject.getId());
        account.setTenantId(dataObject.getTenantId());
        account.setMemberId(dataObject.getMemberId());
        account.setAvailableAmount(defaultAmount(dataObject.getAvailableAmount()));
        account.setFrozenAmount(defaultAmount(dataObject.getFrozenAmount()));
        account.setVersion(dataObject.getVersion() != null ? dataObject.getVersion() : 0L);
        return account;
    }

    private MemberBalanceAccountDO toDO(BalanceAccount account) {
        if (account == null) {
            return null;
        }
        MemberBalanceAccountDO dataObject = new MemberBalanceAccountDO();
        dataObject.setId(account.getId());
        dataObject.setTenantId(account.getTenantId());
        dataObject.setMemberId(account.getMemberId());
        dataObject.setAvailableAmount(account.getAvailableAmount());
        dataObject.setFrozenAmount(account.getFrozenAmount());
        dataObject.setVersion(account.getVersion());
        return dataObject;
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
