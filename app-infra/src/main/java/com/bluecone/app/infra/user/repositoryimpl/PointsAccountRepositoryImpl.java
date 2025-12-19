package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.user.domain.account.PointsAccount;
import com.bluecone.app.core.user.domain.repository.PointsAccountRepository;
import com.bluecone.app.infra.user.dataobject.MemberPointsAccountDO;
import com.bluecone.app.infra.user.mapper.MemberPointsAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 积分账户仓储实现，基于表 bc_member_points_account。
 */
@Repository("corePointsAccountRepositoryImpl")
@RequiredArgsConstructor
public class PointsAccountRepositoryImpl implements PointsAccountRepository {

    private final MemberPointsAccountMapper memberPointsAccountMapper;

    @Override
    public Optional<PointsAccount> findByTenantAndMember(Long tenantId, Long memberId) {
        MemberPointsAccountDO dataObject = memberPointsAccountMapper.selectOne(new LambdaQueryWrapper<MemberPointsAccountDO>()
                .eq(MemberPointsAccountDO::getTenantId, tenantId)
                .eq(MemberPointsAccountDO::getMemberId, memberId));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public PointsAccount save(PointsAccount account) {
        MemberPointsAccountDO dataObject = toDO(account);
        if (account.getId() == null) {
            memberPointsAccountMapper.insert(dataObject);
            account.setId(dataObject.getId());
        } else {
            memberPointsAccountMapper.updateById(dataObject);
        }
        return account;
    }

    @Override
    public boolean saveWithVersion(PointsAccount account, long expectedVersion) {
        MemberPointsAccountDO dataObject = toDO(account);
        LambdaUpdateWrapper<MemberPointsAccountDO> wrapper = new LambdaUpdateWrapper<MemberPointsAccountDO>()
                .eq(MemberPointsAccountDO::getId, account.getId())
                .eq(MemberPointsAccountDO::getVersion, expectedVersion);
        return memberPointsAccountMapper.update(dataObject, wrapper) > 0;
    }

    private PointsAccount toDomain(MemberPointsAccountDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        PointsAccount account = new PointsAccount();
        account.setId(dataObject.getId());
        account.setTenantId(dataObject.getTenantId());
        account.setMemberId(dataObject.getMemberId());
        account.setPointsBalance(dataObject.getPointsBalance() != null ? dataObject.getPointsBalance() : 0);
        account.setFrozenPoints(dataObject.getFrozenPoints() != null ? dataObject.getFrozenPoints() : 0);
        account.setVersion(dataObject.getVersion() != null ? dataObject.getVersion() : 0L);
        return account;
    }

    private MemberPointsAccountDO toDO(PointsAccount account) {
        if (account == null) {
            return null;
        }
        MemberPointsAccountDO dataObject = new MemberPointsAccountDO();
        dataObject.setId(account.getId());
        dataObject.setTenantId(account.getTenantId());
        dataObject.setMemberId(account.getMemberId());
        dataObject.setPointsBalance(account.getPointsBalance());
        dataObject.setFrozenPoints(account.getFrozenPoints());
        dataObject.setVersion(account.getVersion());
        return dataObject;
    }
}
